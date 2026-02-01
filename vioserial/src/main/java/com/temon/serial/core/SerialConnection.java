package com.temon.serial.core;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import com.temon.serial.internal.framing.IdleGapFrameDecoder;
import com.temon.serial.internal.serialport.SerialPort;

import com.temon.serial.codec.HexCodec;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

/**
 * A library-grade serial connection: explicit lifecycle, stable framing, and predictable callbacks.
 */
public final class SerialConnection {

    public enum State {CLOSED, OPENING, OPEN, CLOSING, ERROR}

    private final SerialConfig config;
    private final FrameDecoder frameDecoder;
    private final CallbackDispatcher callbackDispatcher;
    private final SerialLogger logger;
    private final ReconnectPolicy reconnectPolicy;

    private volatile State state = State.CLOSED;
    private volatile SerialListener listener;

    private SerialPort serialPort;
    private InputStream in;
    private OutputStream out;
    private ReadableByteChannel readChannel;  // For NIO mode
    private SelectableChannel selectableChannel;  // For NIO mode
    private Selector selector;  // For NIO mode
    private Thread readThread;
    private Thread reconnectThread;

    private HandlerThread writeThread;
    private Handler writeHandler;
    private long nextSendUptimeMs = 0L;
    private long sessionId = 0L;
    private volatile Throwable lastError;
    private volatile int reconnectAttemptCount = 0;
    private long idleGapMs = 0L;
    private long lastFeedUptimeMs = 0L;
    private Runnable pendingIdleFlush;
    
    // Statistics and monitoring
    private final SerialStatistics statistics = new SerialStatistics();
    private volatile int adaptiveBufferSize;  // Adaptive buffer size

    private SerialConnection(Builder b) {
        this.config = b.config;
        // Protocol-agnostic default: no framing unless caller provides a decoder.
        this.frameDecoder = b.frameDecoder;
        // Professional default: inline callbacks on the read/write threads (most efficient).
        this.callbackDispatcher = b.callbackDispatcher != null
                ? b.callbackDispatcher
                : Dispatchers.direct();
        this.logger = b.logger != null ? b.logger : SerialLogger.NO_OP;
        this.reconnectPolicy = b.reconnectPolicy != null ? b.reconnectPolicy : ReconnectPolicy.NONE;
        this.listener = b.listener;
        this.adaptiveBufferSize = config.readBufferSize;
    }
    
    /**
     * Get statistics and performance metrics.
     */
    public SerialStatistics getStatistics() {
        return statistics;
    }
    
    /**
     * Create a health monitor for this connection.
     * 
     * @param builder HealthMonitor builder with custom thresholds
     * @return HealthMonitor instance
     */
    public HealthMonitor createHealthMonitor(HealthMonitor.Builder builder) {
        return builder.build(this, statistics);
    }
    
    /**
     * Create a default health monitor for this connection.
     */
    public HealthMonitor createHealthMonitor() {
        return new HealthMonitor.Builder().build(this, statistics);
    }

    public static Builder builder(SerialConfig config) {
        return new Builder(config);
    }

    public State getState() {
        return state;
    }

    public boolean isOpen() {
        return state == State.OPEN;
    }

    /**
     * Get the last error that occurred, if any.
     * 
     * @return Last error/exception, or null if no error occurred
     */
    public Throwable getLastError() {
        return lastError;
    }

    public void setListener(SerialListener listener) {
        this.listener = listener;
    }

    public synchronized void open() throws SerialException {
        // Allow opening from CLOSED or ERROR state (recovery)
        if (state != State.CLOSED && state != State.ERROR) {
            throw new SerialException(SerialError.INVALID_PARAMETER, "state must be CLOSED or ERROR, current: " + state);
        }
        State oldState = state;
        state = State.OPENING;
        logger.logStateChange(config.port, oldState, state);
        sessionId++;
        lastError = null;  // Clear previous error on new open attempt
        reconnectAttemptCount = 0;  // Reset reconnect attempts on manual open
        if (frameDecoder != null) {
            frameDecoder.reset();
        }

        try {
            serialPort = new SerialPort(
                    new File(config.port),
                    config.baudRate,
                    config.stopBits,
                    config.dataBits,
                    config.parity,
                    config.flowCon,
                    config.flags,
                    config.readTimeoutMs,
                    config.permissionStrategy
            );
            out = serialPort.getOutputStream();
            in = serialPort.getInputStream();
            if (frameDecoder instanceof IdleGapFrameDecoder) {
                idleGapMs = ((IdleGapFrameDecoder) frameDecoder).getIdleGapMs();
            } else {
                idleGapMs = 0L;
            }

            // Setup NIO if enabled
            if (config.useNioMode && in instanceof FileInputStream) {
                ReadableByteChannel channel = ((FileInputStream) in).getChannel();
                readChannel = channel;
                if (channel instanceof SelectableChannel) {
                    selectableChannel = (SelectableChannel) channel;
                    selectableChannel.configureBlocking(false);
                    selector = Selector.open();
                    selectableChannel.register(selector, SelectionKey.OP_READ);
                    logger.logInfo(config.port, "NIO Selector mode enabled");
                } else {
                    logger.logInfo(config.port, "NIO mode requested but channel is not selectable; falling back to blocking I/O");
                }
            }

            writeThread = new HandlerThread("serial-writer");
            writeThread.start();
            writeHandler = new Handler(writeThread.getLooper());
            nextSendUptimeMs = SystemClock.uptimeMillis();
            
            // Reset statistics on open
            statistics.reset();

            startReadThread();

            state = State.OPEN;
            logger.logStateChange(config.port, State.OPENING, state);
            logger.logInfo(config.port, "Serial port opened successfully");
            dispatchOpen();
        } catch (SecurityException e) {
            lastError = e;
            safeCloseInternal();
            state = State.ERROR;
            logger.logStateChange(config.port, State.OPENING, state);
            logger.logError(config.port, "Permission denied: no rw permission for device node", e);
            throw new SerialException(SerialError.PERMISSION_DENIED, "no rw permission for device node", e);
        } catch (InvalidParameterException e) {
            lastError = e;
            safeCloseInternal();
            state = State.ERROR;
            logger.logStateChange(config.port, State.OPENING, state);
            logger.logError(config.port, "Invalid serial parameters", e);
            throw new SerialException(SerialError.INVALID_PARAMETER, "invalid serial parameters", e);
        } catch (IOException e) {
            lastError = e;
            safeCloseInternal();
            state = State.ERROR;
            logger.logStateChange(config.port, State.OPENING, state);
            logger.logError(config.port, "Failed to open serial port", e);
            throw new SerialException(SerialError.OPEN_FAILED, "failed to open serial port", e);
        } catch (Throwable t) {
            lastError = t;
            safeCloseInternal();
            state = State.ERROR;
            logger.logStateChange(config.port, State.OPENING, state);
            logger.logError(config.port, "Unexpected error when opening serial port", t);
            throw new SerialException(SerialError.OPEN_FAILED, "unexpected error when opening serial port", t);
        }
    }

    public synchronized void close() {
        if (state == State.CLOSED) return;
        State oldState = state;
        state = State.CLOSING;
        logger.logStateChange(config.port, oldState, state);
        sessionId++;
        reconnectAttemptCount = 0;  // Reset reconnect attempts
        safeCloseInternal();
        state = State.CLOSED;
        logger.logStateChange(config.port, State.CLOSING, state);
        logger.logInfo(config.port, "Serial port closed");
        dispatchClose();
    }

    public void sendBytes(final byte[] data) throws SerialException {
        if (data == null) throw new SerialException(SerialError.INVALID_PARAMETER, "data == null");
        if (state != State.OPEN || writeHandler == null) {
            if (state == State.ERROR) {
                throw new SerialException(SerialError.IO_ERROR, "connection is in ERROR state");
            }
            throw new SerialException(SerialError.CLOSED, "connection is not open");
        }
        final byte[] payload = Arrays.copyOf(data, data.length);
        final long session = sessionId;
        final long now = SystemClock.uptimeMillis();
        final long when;
        synchronized (this) {
            when = Math.max(now, nextSendUptimeMs);
            nextSendUptimeMs = when + config.sendIntervalMs;
        }
        writeHandler.postAtTime(new Runnable() {
            @Override
            public void run() {
                if (session != sessionId) {
                    statistics.onWriteError();
                    logger.logError(config.port, "Write dropped: session changed before send", null);
                    return;
                }
                if (state != State.OPEN) {
                    statistics.onWriteError();
                    logger.logError(config.port, "Write dropped: connection not open", null);
                    dispatchError(session, new IOException("Write dropped: connection not open"));
                    return;
                }
                try {
                    out.write(payload);
                    statistics.onBytesSent(payload.length);
                    if (logger.isEnabled()) {
                        logger.logTxBytes(config.port, payload, payload.length);
                    }
                } catch (IOException e) {
                    statistics.onWriteError();
                    // IO exception during write: device may be disconnected
                    lastError = e;
                    synchronized (SerialConnection.this) {
                        if (session == sessionId && state == State.OPEN) {
                            State oldState = state;
                            state = State.ERROR;
                            logger.logStateChange(config.port, oldState, state);
                            logger.logError(config.port, "IO exception during write, device may be disconnected", e);
                            safeCloseInternal();
                        }
                    }
                    dispatchError(session, e);
                    startReconnectIfNeeded();
                } catch (Throwable t) {
                    lastError = t;
                    synchronized (SerialConnection.this) {
                        if (session == sessionId && state == State.OPEN) {
                            State oldState = state;
                            state = State.ERROR;
                            logger.logStateChange(config.port, oldState, state);
                            logger.logError(config.port, "Unexpected error during write", t);
                            safeCloseInternal();
                        }
                    }
                    dispatchError(session, t);
                    startReconnectIfNeeded();
                }
            }
        }, when);
    }

    public void sendHex(String hex) throws SerialException {
        if (hex == null) throw new SerialException(SerialError.INVALID_PARAMETER, "hex == null");
        String cleaned = hex.trim().replace(" ", "");
        try {
            sendBytes(HexCodec.decode(cleaned));
        } catch (IllegalArgumentException e) {
            throw new SerialException(SerialError.INVALID_PARAMETER, "invalid hex string", e);
        }
    }

    public void sendText(String text) throws SerialException {
        if (text == null) throw new SerialException(SerialError.INVALID_PARAMETER, "text == null");
        sendBytes(text.getBytes(config.textCharset));
    }

    private void startReadThread() {
        final long session = sessionId;
        readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (config.useNioMode && selector != null) {
                    runNioMode(session);
                } else {
                    runBlockingMode(session);
                }
            }
            
            private void runNioMode(final long session) {
                ByteBuffer buffer = ByteBuffer.allocate(adaptiveBufferSize);
                long lastDeviceCheck = SystemClock.uptimeMillis();
                long lastAdaptiveAdjust = SystemClock.uptimeMillis();
                
                try {
                    while (session == sessionId && state == State.OPEN && !Thread.currentThread().isInterrupted()) {
                        // Periodic device online check
                        if (config.deviceCheckIntervalMs > 0) {
                            long now = SystemClock.uptimeMillis();
                            if (now - lastDeviceCheck >= config.deviceCheckIntervalMs) {
                                lastDeviceCheck = now;
                                if (serialPort != null && !serialPort.isDeviceOnline()) {
                                    handleDeviceDisconnected(session);
                                    break;
                                }
                            }
                        }
                        
                        // Adaptive buffer size adjustment (every 10 seconds)
                        long now = SystemClock.uptimeMillis();
                        if (now - lastAdaptiveAdjust >= 10000) {
                            adjustBufferSize();
                            lastAdaptiveAdjust = now;
                            // Reallocate buffer if size changed
                            if (buffer.capacity() != adaptiveBufferSize) {
                                buffer = ByteBuffer.allocate(adaptiveBufferSize);
                            }
                        }
                        
                        // Select with timeout
                        int timeout = config.readTimeoutMs > 0 ? config.readTimeoutMs : 1000;
                        int selected = selector.select(timeout);
                        
                        if (selected == 0) {
                            // Timeout - check device if configured
                            if (config.readTimeoutMs > 0 && serialPort != null) {
                                if (!serialPort.isDeviceOnline()) {
                                    handleDeviceDisconnected(session);
                                    break;
                                }
                            }
                            continue;
                        }
                        
                        Set<SelectionKey> keys = selector.selectedKeys();
                        Iterator<SelectionKey> keyIterator = keys.iterator();
                        
                        while (keyIterator.hasNext()) {
                            SelectionKey key = keyIterator.next();
                            keyIterator.remove();
                            
                            if (key.isReadable()) {
                                buffer.clear();
                                int n = readChannel.read(buffer);
                                
                                if (n < 0) {
                                    // EOF - device disconnected
                                    handleDeviceDisconnected(session);
                                    break;
                                } else if (n > 0) {
                                    buffer.flip();
                                    byte[] chunk = new byte[n];
                                    buffer.get(chunk);
                                    
                                    statistics.onBytesReceived(n);
                                    if (logger.isEnabled()) {
                                        logger.logRxBytes(config.port, chunk, n);
                                    }
                                    dispatchBytes(session, chunk, n);
                                    
                                    if (frameDecoder != null) {
                                        frameDecoder.feed(chunk, 0, n, new FrameDecoder.FrameCallback() {
                                            @Override
                                            public void onFrame(byte[] frameBytes, int length) {
                                                statistics.onFrameReceived();
                                                dispatchFrame(session, frameBytes, length);
                                            }
                                        });
                                        scheduleIdleFlush(session);
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    handleReadError(session, e);
                } catch (Throwable t) {
                    handleReadError(session, t);
                } finally {
                    logger.logInfo(config.port, "NIO read thread exiting");
                }
            }
            
            private void runBlockingMode(final long session) {
                byte[] buf = new byte[adaptiveBufferSize];
                long lastDeviceCheck = SystemClock.uptimeMillis();
                long lastAdaptiveAdjust = SystemClock.uptimeMillis();
                
                try {
                    while (session == sessionId && state == State.OPEN && !Thread.currentThread().isInterrupted()) {
                        // Periodic device online check
                        if (config.deviceCheckIntervalMs > 0) {
                            long now = SystemClock.uptimeMillis();
                            if (now - lastDeviceCheck >= config.deviceCheckIntervalMs) {
                                lastDeviceCheck = now;
                                if (serialPort != null && !serialPort.isDeviceOnline()) {
                                    // Device disconnected
                                    synchronized (SerialConnection.this) {
                                        if (session == sessionId && state == State.OPEN) {
                                            State oldState = state;
                                            state = State.ERROR;
                                            logger.logStateChange(config.port, oldState, state);
                                            logger.logError(config.port, "Device disconnected (detected by periodic check)", null);
                                            safeCloseInternal();
                                            dispatchError(session, new IOException("Device disconnected"));
                                            // Trigger reconnection if policy is enabled
                                            startReconnectIfNeeded();
                                        }
                                    }
                                    break;
                                }
                            }
                        }

                        // Adaptive buffer size adjustment (every 10 seconds)
                        long now = SystemClock.uptimeMillis();
                        if (now - lastAdaptiveAdjust >= 10000) {
                            adjustBufferSize();
                            lastAdaptiveAdjust = now;
                            // Reallocate buffer if size changed
                            if (buf.length != adaptiveBufferSize) {
                                buf = new byte[adaptiveBufferSize];
                            }
                        }
                        
                        int n = in.read(buf);
                        if (n <= 0) {
                            // If read returns 0 and we have timeout configured, check device
                            if (n == 0 && config.readTimeoutMs > 0 && serialPort != null) {
                                if (!serialPort.isDeviceOnline()) {
                                    handleDeviceDisconnected(session);
                                    break;
                                }
                            }
                            continue;
                        }

                        byte[] chunk = Arrays.copyOf(buf, n);
                        statistics.onBytesReceived(n);
                        if (logger.isEnabled()) {
                            logger.logRxBytes(config.port, chunk, n);
                        }
                        dispatchBytes(session, chunk, n);

                        if (frameDecoder != null) {
                            frameDecoder.feed(buf, 0, n, new FrameDecoder.FrameCallback() {
                                @Override
                                public void onFrame(byte[] frameBytes, int length) {
                                    statistics.onFrameReceived();
                                    dispatchFrame(session, frameBytes, length);
                                }
                            });
                            scheduleIdleFlush(session);
                        }
                    }
                } catch (IOException e) {
                    handleReadError(session, e);
                } catch (Throwable t) {
                    handleReadError(session, t);
                } finally {
                    logger.logInfo(config.port, "Blocking read thread exiting");
                }
            }
            
            private void handleDeviceDisconnected(final long session) {
                synchronized (SerialConnection.this) {
                    if (session == sessionId && state == State.OPEN) {
                        State oldState = state;
                        state = State.ERROR;
                        logger.logStateChange(config.port, oldState, state);
                        logger.logError(config.port, "Device disconnected", null);
                        safeCloseInternal();
                        dispatchError(session, new IOException("Device disconnected"));
                        startReconnectIfNeeded();
                    }
                }
            }
            
            private void handleReadError(final long session, Throwable t) {
                lastError = t;
                statistics.onReadError();
                if (session == sessionId && state == State.OPEN) {
                    synchronized (SerialConnection.this) {
                        if (session == sessionId && state == State.OPEN) {
                            State oldState = state;
                            state = State.ERROR;
                            logger.logStateChange(config.port, oldState, state);
                            logger.logError(config.port, "Error during read", t);
                            safeCloseInternal();
                            startReconnectIfNeeded();
                        }
                    }
                }
                dispatchError(session, t);
            }
        }, "serial-reader");
        readThread.start();
    }

    
    /**
     * Adaptive buffer size adjustment based on throughput.
     * Increases buffer size for high-throughput scenarios (e.g., automotive CAN bus).
     */
    private synchronized void adjustBufferSize() {
        double throughput = statistics.getReceiveThroughputBps();
        int currentSize = adaptiveBufferSize;
        int minSize = 512;
        int maxSize = 8192;
        
        // Adjust based on throughput
        if (throughput > 10000) {  // > 10 KB/s - high throughput
            adaptiveBufferSize = Math.min(maxSize, currentSize * 2);
        } else if (throughput > 1000) {  // > 1 KB/s - medium throughput
            adaptiveBufferSize = Math.min(maxSize, (currentSize * 3) / 2);
        } else if (throughput < 100) {  // < 100 B/s - low throughput
            adaptiveBufferSize = Math.max(minSize, (currentSize * 2) / 3);
        }
        
        if (adaptiveBufferSize != currentSize) {
            logger.logInfo(config.port, String.format(
                "Buffer size adjusted: %d -> %d (throughput: %.2f B/s)",
                currentSize, adaptiveBufferSize, throughput
            ));
        }
    }

    private void scheduleIdleFlush(final long session) {
        if (idleGapMs <= 0 || writeHandler == null) return;
        lastFeedUptimeMs = SystemClock.uptimeMillis();
        if (pendingIdleFlush != null) {
            writeHandler.removeCallbacks(pendingIdleFlush);
        }
        pendingIdleFlush = new Runnable() {
            @Override
            public void run() {
                if (session != sessionId || state != State.OPEN) return;
                long now = SystemClock.uptimeMillis();
                if (now - lastFeedUptimeMs >= idleGapMs) {
                    flushPendingFrameIfNeeded(session);
                }
            }
        };
        writeHandler.postDelayed(pendingIdleFlush, idleGapMs);
    }

    private void flushPendingFrameIfNeeded(final long session) {
        if (frameDecoder instanceof FlushableFrameDecoder) {
            ((FlushableFrameDecoder) frameDecoder).flush(new FrameDecoder.FrameCallback() {
                @Override
                public void onFrame(byte[] frameBytes, int length) {
                    dispatchFrame(session, frameBytes, length);
                }
            });
        }
    }

    private void safeCloseInternal() {
        // Stop reconnect thread if running
        if (reconnectThread != null) {
            reconnectThread.interrupt();
            reconnectThread = null;
        }
        if (writeHandler != null) {
            writeHandler.removeCallbacksAndMessages(null);
        }
        
        // Close NIO resources
        if (selector != null) {
            try {
                selector.close();
            } catch (Throwable t) {
                logger.logError(config.port, "Failed to close selector", t);
            }
            selector = null;
        }
        if (readChannel != null) {
            try {
                readChannel.close();
            } catch (Throwable t) {
                logger.logError(config.port, "Failed to close read channel", t);
            }
            readChannel = null;
        }
        selectableChannel = null;
        
        if (readThread != null) {
            readThread.interrupt();
            // Close InputStream to unblock read() if it's blocking
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Throwable t) {
                logger.logError(config.port, "Failed to close input stream during shutdown", t);
            }
            // Wake up selector if in NIO mode
            if (selector != null) {
                selector.wakeup();
            }
            // Wait for thread to exit with timeout (5 seconds), unless we're on the read thread.
            if (Thread.currentThread() != readThread) {
                try {
                    readThread.join(5000);
                    if (readThread.isAlive()) {
                        logger.logError(config.port, "Read thread did not exit within timeout", null);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.logError(config.port, "Interrupted while waiting for read thread to exit", e);
                }
            }
        }
        // Flush last pending frame if decoder supports it (useful for idle-gap framing).
        if (frameDecoder instanceof FlushableFrameDecoder) {
            final long session = sessionId;
            ((FlushableFrameDecoder) frameDecoder).flush(new FrameDecoder.FrameCallback() {
                @Override
                public void onFrame(byte[] frameBytes, int length) {
                    dispatchFrame(session, frameBytes, length);
                }
            });
        }
        try {
            if (in != null) in.close();
        } catch (Throwable t) {
            logger.logError(config.port, "Failed to close input stream", t);
        }
        try {
            if (out != null) out.close();
        } catch (Throwable t) {
            logger.logError(config.port, "Failed to close output stream", t);
        }
        try {
            if (serialPort != null) serialPort.close();
        } catch (Throwable t) {
            logger.logError(config.port, "Failed to close serial port", t);
        }
        serialPort = null;
        in = null;
        out = null;
        readThread = null;

        if (writeThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                writeThread.quitSafely();
            } else {
                writeThread.quit();
            }
            if (Thread.currentThread() != writeThread) {
                try {
                    writeThread.join(5000);
                    if (writeThread.isAlive()) {
                        logger.logError(config.port, "Write thread did not exit within timeout", null);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.logError(config.port, "Interrupted while waiting for write thread to exit", e);
                }
            }
        }
        writeThread = null;
        writeHandler = null;
        pendingIdleFlush = null;
        if (frameDecoder != null) {
            frameDecoder.reset();
        }
    }

    private void dispatchOpen() {
        final long session = sessionId;
        callbackDispatcher.dispatch(new Runnable() {
            @Override
            public void run() {
                if (session != sessionId) return;
                SerialListener l = listener;
                if (l != null) l.onOpen();
            }
        });
    }

    private void dispatchClose() {
        final long session = sessionId;
        callbackDispatcher.dispatch(new Runnable() {
            @Override
            public void run() {
                if (session != sessionId) return;
                SerialListener l = listener;
                if (l != null) l.onClose();
            }
        });
    }

    private void dispatchBytes(final long session, final byte[] data, final int len) {
        callbackDispatcher.dispatch(new Runnable() {
            @Override
            public void run() {
                if (session != sessionId) return;
                SerialListener l = listener;
                if (l != null) l.onBytes(data, len);
            }
        });
    }

    private void dispatchFrame(final long session, final byte[] frame, final int len) {
        if (logger.isEnabled()) {
            logger.logFrame(config.port, frame, len);
        }
        callbackDispatcher.dispatch(new Runnable() {
            @Override
            public void run() {
                if (session != sessionId) return;
                SerialListener l = listener;
                if (l != null) l.onFrame(frame, len);
            }
        });
    }

    private void dispatchError(final long session, final Throwable t) {
        callbackDispatcher.dispatch(new Runnable() {
            @Override
            public void run() {
                if (session != sessionId) return;
                SerialListener l = listener;
                if (l != null) l.onError(t);
            }
        });
    }

    private synchronized void startReconnectIfNeeded() {
        // Only start reconnect if we're in ERROR state and policy is enabled
        if (state != State.ERROR || reconnectPolicy == ReconnectPolicy.NONE) {
            return;
        }
        // Don't start multiple reconnect threads
        if (reconnectThread != null && reconnectThread.isAlive()) {
            return;
        }
        reconnectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        int attempt;
                        Throwable errorSnapshot;
                        synchronized (SerialConnection.this) {
                            if (state != State.ERROR) {
                                return;
                            }
                            attempt = ++reconnectAttemptCount;
                            errorSnapshot = lastError;
                        }

                        if (!reconnectPolicy.shouldReconnect(attempt, errorSnapshot)) {
                            logger.logInfo(config.port, "Reconnection stopped by policy after " + attempt + " attempts");
                            reconnectPolicy.onReconnectFailed(attempt, errorSnapshot);
                            return;
                        }

                        long delay = reconnectPolicy.getReconnectDelayMs(attempt);
                        logger.logInfo(config.port, "Starting reconnection attempt " + attempt + " after " + delay + "ms");
                        if (delay > 0) {
                            Thread.sleep(delay);
                        }

                        synchronized (SerialConnection.this) {
                            if (state != State.ERROR) {
                                return;
                            }
                            try {
                                logger.logInfo(config.port, "Attempting to reconnect...");
                                state = State.OPENING;
                                logger.logStateChange(config.port, State.ERROR, state);
                                sessionId++;
                                lastError = null;

                                if (frameDecoder != null) {
                                    frameDecoder.reset();
                                }

                                serialPort = new SerialPort(
                                        new File(config.port),
                                        config.baudRate,
                                        config.stopBits,
                                        config.dataBits,
                                        config.parity,
                                        config.flowCon,
                                        config.flags,
                                        config.readTimeoutMs,
                                        config.permissionStrategy
                                );
                                out = serialPort.getOutputStream();
                                in = serialPort.getInputStream();

                                if (config.useNioMode && in instanceof FileInputStream) {
                                    ReadableByteChannel channel = ((FileInputStream) in).getChannel();
                                    readChannel = channel;
                                    if (channel instanceof SelectableChannel) {
                                        selectableChannel = (SelectableChannel) channel;
                                        selectableChannel.configureBlocking(false);
                                        selector = Selector.open();
                                        selectableChannel.register(selector, SelectionKey.OP_READ);
                                        logger.logInfo(config.port, "NIO Selector mode enabled");
                                    } else {
                                        logger.logInfo(config.port, "NIO mode requested but channel is not selectable; falling back to blocking I/O");
                                    }
                                }

                                writeThread = new HandlerThread("serial-writer");
                                writeThread.start();
                                writeHandler = new Handler(writeThread.getLooper());
                                nextSendUptimeMs = SystemClock.uptimeMillis();

                                startReadThread();

                                state = State.OPEN;
                                logger.logStateChange(config.port, State.OPENING, state);
                                logger.logInfo(config.port, "Reconnection successful");
                                statistics.onReconnect();
                                reconnectPolicy.onReconnectSuccess(attempt);
                                reconnectAttemptCount = 0;
                                dispatchOpen();
                                return;
                            } catch (Throwable t) {
                                lastError = t;
                                safeCloseInternal();
                                state = State.ERROR;
                                logger.logStateChange(config.port, State.OPENING, state);
                                logger.logError(config.port, "Reconnection failed", t);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.logInfo(config.port, "Reconnection thread interrupted");
                } catch (Throwable t) {
                    logger.logError(config.port, "Unexpected error in reconnection thread", t);
                }
            }
        }, "serial-reconnect");
        reconnectThread.start();
    }

    public static final class Builder {
        private final SerialConfig config;
        private FrameDecoder frameDecoder;
        private CallbackDispatcher callbackDispatcher;
        private SerialListener listener;
        private SerialLogger logger;
        private ReconnectPolicy reconnectPolicy;

        private Builder(SerialConfig config) {
            if (config == null) throw new IllegalArgumentException("config == null");
            this.config = config;
        }

        public Builder frameDecoder(FrameDecoder decoder) {
            this.frameDecoder = decoder;
            return this;
        }

        public Builder callbackDispatcher(CallbackDispatcher dispatcher) {
            this.callbackDispatcher = dispatcher;
            return this;
        }

        public Builder listener(SerialListener listener) {
            this.listener = listener;
            return this;
        }

        /**
         * Set logger for diagnostics. Default: NO_OP (disabled).
         * 
         * @param logger Logger instance, or null to disable
         */
        public Builder logger(SerialLogger logger) {
            this.logger = logger;
            return this;
        }

        /**
         * Set reconnection policy. Default: NONE (disabled).
         * 
         * <p>When enabled, the connection will automatically attempt to reconnect
         * when it enters ERROR state, according to the policy rules.</p>
         * 
         * @param policy Reconnection policy, or null to disable
         */
        public Builder reconnectPolicy(ReconnectPolicy policy) {
            this.reconnectPolicy = policy;
            return this;
        }

        public SerialConnection build() {
            return new SerialConnection(this);
        }
    }
}



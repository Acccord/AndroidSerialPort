package com.temon.serial.core;

import com.temon.serial.codec.HexCodec;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Serial port manager supporting multiple ports and multiple instances.
 *
 * <p>Use the serial port path (e.g. "/dev/ttyS1") as the key. Multiple ports can be opened at once.</p>
 *
 * <p>This class supports multiple instances for complete isolation. Each instance manages its own
 * set of serial connections independently.</p>
 *
 * <p><b>Recommended usage:</b> Create instances with {@code new SerialManager()}.</p>
 */
public final class SerialManager {
    /**
     * Create a new SerialManager instance.
     *
     * <p>Each instance manages its own set of serial connections independently,
     * allowing multiple isolated serial port managers in the same application.</p>
     */
    public SerialManager() {
    }

    public interface OnHexDataListener {
        /**
         * A hex string (no spaces). In framed mode, it's a complete frame.
         * In RAW mode, it's a hex chunk from the stream.
         */
        void onData(String hex);
    }

    public interface OnBytesListener {
        /**
         * Raw bytes chunk from the underlying stream.
         */
        void onBytes(byte[] data, int length);
    }

    public interface OnFrameListener {
        /**
         * A complete frame decoded by the frame decoder.
         */
        void onFrame(byte[] frame, int length);
    }

    public interface OnErrorListener {
        /**
         * Called when an error occurs on the serial port.
         *
         * @param port Serial port path
         * @param error Error code
         * @param message Friendly error message
         * @param throwable Exception (may be null)
         */
        void onError(String port, SerialError error, String message, Throwable throwable);
    }

    private static final class ErrorInfo {
        final SerialError error;
        final String message;

        ErrorInfo(SerialError error, String message) {
            this.error = error;
            this.message = message;
        }
    }

    private final Object lock = new Object();
    private final ConcurrentHashMap<String, SerialConnection> connections = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<OnHexDataListener>> listeners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<OnBytesListener>> byteListeners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<OnFrameListener>> frameListeners = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<OnErrorListener> errorListeners = new CopyOnWriteArrayList<>();

    private volatile FrameDecoder defaultDecoder = null;
    private volatile ReconnectPolicy defaultReconnectPolicy = ReconnectPolicy.NONE;
    private volatile CallbackDispatcher callbackDispatcher = Dispatchers.direct();
    private volatile SerialLogger logger = SerialLogger.NO_OP;

    /**
     * Chain-style configuration (fluent).
     */
    public Configurator config() {
        return new Configurator(this);
    }

    /**
     * Configure callback dispatcher for all connections opened by this manager.
     * Default: {@link Dispatchers#direct()} (callbacks on IO thread).
     */
    public void setCallbackDispatcher(CallbackDispatcher dispatcher) {
        this.callbackDispatcher = dispatcher != null ? dispatcher : Dispatchers.direct();
    }

    public CallbackDispatcher getCallbackDispatcher() {
        return callbackDispatcher;
    }

    /**
     * Configure default logger for connections opened by this manager.
     */
    public void setLogger(SerialLogger logger) {
        this.logger = logger != null ? logger : SerialLogger.NO_OP;
    }

    public SerialLogger getLogger() {
        return logger;
    }

    /**
     * Configure default framing strategy for {@link #open(String, int)}.
     * Use null to disable framing (raw mode).
     */
    public void setDefaultFrameDecoder(FrameDecoder decoder) {
        this.defaultDecoder = decoder;
    }

    /**
     * Configure default reconnection policy for {@link #open(String, int)}.
     */
    public void setDefaultReconnectPolicy(ReconnectPolicy policy) {
        this.defaultReconnectPolicy = policy != null ? policy : ReconnectPolicy.NONE;
    }

    /**
     * Convenience: use CRLF framing.
     */
    public void useDefaultCrlf() {
        this.defaultDecoder = SerialFraming.crlf();
    }

    /**
     * Open serial port with a prepared config + decoder.
     */
    public boolean open(SerialConfig config, FrameDecoder decoder) throws SerialException {
        if (config == null) throw new SerialException(SerialError.INVALID_PARAMETER, "config == null");
        return openInternal(config, decoder);
    }

    /**
     * Open serial port with default framing (per port key).
     *
     * @return true if opened successfully
     * @throws SerialException on invalid params / permission / open failure
     */
    public boolean open(String port, int baudRate) throws SerialException {
        SerialConfig config = new SerialConfig.Builder()
                .port(port)
                .baudRate(baudRate)
                .build();
        return openInternal(config, defaultDecoder);
    }

    public boolean isOpen(String port) {
        SerialConnection c = connections.get(port);
        return c != null && c.isOpen();
    }

    public void close(String port) {
        SerialConnection c = connections.remove(port);
        if (c != null) c.close();
        listeners.remove(port);
        byteListeners.remove(port);
        frameListeners.remove(port);
    }

    /**
     * Close all ports.
     */
    public void closeAll() {
        for (String port : connections.keySet()) {
            close(port);
        }
    }

    /**
     * Send HEX string to the opened serial connection (by port).
     */
    public void sendHex(String port, String hex) throws SerialException {
        SerialConnection c = connections.get(port);
        if (c == null || !c.isOpen()) {
            throw new SerialException(SerialError.CLOSED, "serial is not open: " + port);
        }
        c.sendHex(hex);
    }

    public void sendBytes(String port, byte[] data) throws SerialException {
        SerialConnection c = connections.get(port);
        if (c == null || !c.isOpen()) {
            throw new SerialException(SerialError.CLOSED, "serial is not open: " + port);
        }
        c.sendBytes(data);
    }

    public void addDataListener(String port, OnHexDataListener listener) {
        if (listener == null || port == null) return;
        getOrCreateList(listeners, port).addIfAbsent(listener);
    }

    public void removeDataListener(String port, OnHexDataListener listener) {
        if (listener == null || port == null) return;
        CopyOnWriteArrayList<OnHexDataListener> list = listeners.get(port);
        if (list != null) {
            list.remove(listener);
        }
    }

    public void clearDataListeners(String port) {
        CopyOnWriteArrayList<OnHexDataListener> list = listeners.get(port);
        if (list != null) {
            list.clear();
        }
    }

    public void addBytesListener(String port, OnBytesListener listener) {
        if (listener == null || port == null) return;
        getOrCreateList(byteListeners, port).addIfAbsent(listener);
    }

    public void removeBytesListener(String port, OnBytesListener listener) {
        if (listener == null || port == null) return;
        CopyOnWriteArrayList<OnBytesListener> list = byteListeners.get(port);
        if (list != null) {
            list.remove(listener);
        }
    }

    public void clearBytesListeners(String port) {
        CopyOnWriteArrayList<OnBytesListener> list = byteListeners.get(port);
        if (list != null) {
            list.clear();
        }
    }

    public void addFrameListener(String port, OnFrameListener listener) {
        if (listener == null || port == null) return;
        getOrCreateList(frameListeners, port).addIfAbsent(listener);
    }

    public void removeFrameListener(String port, OnFrameListener listener) {
        if (listener == null || port == null) return;
        CopyOnWriteArrayList<OnFrameListener> list = frameListeners.get(port);
        if (list != null) {
            list.remove(listener);
        }
    }

    public void clearFrameListeners(String port) {
        CopyOnWriteArrayList<OnFrameListener> list = frameListeners.get(port);
        if (list != null) {
            list.clear();
        }
    }

    public void addErrorListener(OnErrorListener listener) {
        if (listener != null) {
            errorListeners.addIfAbsent(listener);
        }
    }

    public void removeErrorListener(OnErrorListener listener) {
        if (listener != null) {
            errorListeners.remove(listener);
        }
    }

    public void clearErrorListeners() {
        errorListeners.clear();
    }

    private static <T> CopyOnWriteArrayList<T> getOrCreateList(
            ConcurrentHashMap<String, CopyOnWriteArrayList<T>> map,
            String port
    ) {
        CopyOnWriteArrayList<T> list = map.get(port);
        if (list == null) {
            CopyOnWriteArrayList<T> newList = new CopyOnWriteArrayList<>();
            CopyOnWriteArrayList<T> existing = map.putIfAbsent(port, newList);
            list = existing != null ? existing : newList;
        }
        return list;
    }

    private boolean openInternal(SerialConfig config, FrameDecoder decoder) throws SerialException {
        final String port = config.port;
        if (port == null || port.trim().isEmpty()) {
            throw new SerialException(SerialError.INVALID_PARAMETER, "port cannot be empty");
        }
        final boolean rawMode = decoder == null;
        synchronized (lock) {
            SerialConnection existing = connections.remove(port);
            if (existing != null) {
                existing.close();
            }
            SerialConnection c = SerialConnection.builder(config)
                    .frameDecoder(decoder) // may be null (raw)
                    .reconnectPolicy(defaultReconnectPolicy)
                    .callbackDispatcher(callbackDispatcher)
                    .logger(logger)
                    .listener(new SerialListenerAdapter() {
                        @Override
                        public void onBytes(byte[] data, int length) {
                            if (length <= 0 || data == null) return;
                            CopyOnWriteArrayList<OnBytesListener> list = byteListeners.get(port);
                            if (list != null) {
                                for (OnBytesListener l : list) {
                                    try {
                                        l.onBytes(data, length);
                                    } catch (Throwable t) {
                                        // Protect IO thread from listener errors
                                        if (logger.isEnabled()) {
                                            logger.logError(port, "OnBytesListener threw", t);
                                        }
                                    }
                                }
                            }
                            if (rawMode) {
                                String hex = HexCodec.encode(data, 0, length);
                                CopyOnWriteArrayList<OnHexDataListener> hexList = listeners.get(port);
                                if (hexList != null) {
                                    for (OnHexDataListener l : hexList) {
                                        try {
                                            l.onData(hex);
                                        } catch (Throwable t) {
                                            // Protect IO thread from listener errors
                                            if (logger.isEnabled()) {
                                                logger.logError(port, "OnHexDataListener threw", t);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        @Override
                        public void onFrame(byte[] frame, int length) {
                            if (length <= 0) return;
                            CopyOnWriteArrayList<OnFrameListener> frameList = frameListeners.get(port);
                            if (frameList != null) {
                                for (OnFrameListener l : frameList) {
                                    try {
                                        l.onFrame(frame, length);
                                    } catch (Throwable t) {
                                        // Protect IO thread from listener errors
                                        if (logger.isEnabled()) {
                                            logger.logError(port, "OnFrameListener threw", t);
                                        }
                                    }
                                }
                            }
                            String hex = HexCodec.encode(frame, 0, length);
                            CopyOnWriteArrayList<OnHexDataListener> list = listeners.get(port);
                            if (list != null) {
                                for (OnHexDataListener l : list) {
                                    try {
                                        l.onData(hex);
                                    } catch (Throwable t) {
                                        // Protect IO thread from listener errors
                                        if (logger.isEnabled()) {
                                            logger.logError(port, "OnHexDataListener threw", t);
                                        }
                                    }
                                }
                            }
                        }

                        @Override
                        public void onError(Throwable t) {
                            ErrorInfo info = buildErrorInfo(t);
                            if (logger.isEnabled()) {
                                logger.logError(port, info.message, t);
                            }
                            for (OnErrorListener l : errorListeners) {
                                try {
                                    l.onError(port, info.error, info.message, t);
                                } catch (Throwable t2) {
                                    // Don't let listener exceptions break the system
                                    if (logger.isEnabled()) {
                                        logger.logError(port, "OnErrorListener threw", t2);
                                    }
                                }
                            }
                        }
                    })
                    .build();
            c.open();
            connections.put(port, c);
            return true;
        }
    }

    private static ErrorInfo buildErrorInfo(Throwable t) {
        if (t == null) {
            return new ErrorInfo(SerialError.OPEN_FAILED, "Unknown error occurred");
        }
        if (t instanceof SerialException) {
            SerialException se = (SerialException) t;
            switch (se.getError()) {
                case PERMISSION_DENIED:
                    return new ErrorInfo(SerialError.PERMISSION_DENIED,
                            "Permission denied. Ensure device node has rw permission or use a permission strategy.");
                case INVALID_PARAMETER:
                    return new ErrorInfo(SerialError.INVALID_PARAMETER, "Invalid parameter. Check port path and baud rate.");
                case OPEN_FAILED:
                    return new ErrorInfo(SerialError.OPEN_FAILED, "Open failed. Check device path and permission.");
                case CLOSED:
                    return new ErrorInfo(SerialError.CLOSED, "Serial is closed. Call open() before sending.");
                case IO_ERROR:
                default:
                    return new ErrorInfo(SerialError.IO_ERROR, "IO error. Device may be disconnected.");
            }
        }
        if (t instanceof SecurityException) {
            return new ErrorInfo(SerialError.PERMISSION_DENIED, "Permission denied. Check device node permissions.");
        }
        if (t instanceof IOException) {
            String msg = t.getMessage();
            if (msg != null && msg.toLowerCase().contains("disconnect")) {
                return new ErrorInfo(SerialError.IO_ERROR, "Device disconnected.");
            }
            return new ErrorInfo(SerialError.IO_ERROR, "IO error: " + (msg != null ? msg : "unknown"));
        }
        return new ErrorInfo(SerialError.OPEN_FAILED, "Error: " + t.getMessage());
    }

    public static final class Configurator {
        private final SerialManager target;
        private final SerialConfig.Builder configBuilder = new SerialConfig.Builder();
        private FrameDecoder decoder = null;

        private Configurator(SerialManager target) {
            this.target = target;
        }

        public Configurator stopBits(int stopBits) {
            configBuilder.stopBits(stopBits);
            return this;
        }

        public Configurator dataBits(int dataBits) {
            configBuilder.dataBits(dataBits);
            return this;
        }

        public Configurator parity(int parity) {
            configBuilder.parity(parity);
            return this;
        }

        public Configurator flowCon(int flowCon) {
            configBuilder.flowCon(flowCon);
            return this;
        }

        public Configurator flags(int flags) {
            configBuilder.flags(flags);
            return this;
        }

        public Configurator sendIntervalMs(int intervalMs) {
            configBuilder.sendIntervalMs(intervalMs);
            return this;
        }

        public Configurator frameDecoder(FrameDecoder decoder) {
            this.decoder = decoder;
            return this;
        }

        public Configurator callbackDispatcher(CallbackDispatcher dispatcher) {
            target.setCallbackDispatcher(dispatcher);
            return this;
        }

        public Configurator logger(SerialLogger logger) {
            target.setLogger(logger);
            return this;
        }

        public Configurator raw() {
            this.decoder = null;
            return this;
        }

        public Configurator crlf() {
            this.decoder = SerialFraming.crlf();
            return this;
        }

        public Configurator delimiter(byte[] delimiter, boolean includeDelimiter) {
            this.decoder = SerialFraming.delimiter(delimiter, includeDelimiter);
            return this;
        }

        public Configurator fixedLength(int length) {
            this.decoder = SerialFraming.fixedLength(length);
            return this;
        }

        public Configurator lengthField(SerialFraming.LengthFieldBuilder builder) {
            if (builder == null) {
                this.decoder = null;
            } else {
                this.decoder = builder.build();
            }
            return this;
        }

        public Configurator idleGap(long idleGapMs, int maxFrameLength) {
            this.decoder = SerialFraming.idleGap(idleGapMs, maxFrameLength);
            return this;
        }

        public boolean open() throws SerialException {
            return target.open(configBuilder.build(), decoder);
        }

        /**
         * Shortcut: open with required params in a single call.
         */
        public boolean open(String port, int baudRate) throws SerialException {
            configBuilder.port(port);
            configBuilder.baudRate(baudRate);
            return open();
        }
    }
}

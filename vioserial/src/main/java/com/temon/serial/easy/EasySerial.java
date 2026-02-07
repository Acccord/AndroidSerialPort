package com.temon.serial.easy;

import com.temon.serial.core.Dispatchers;
import com.temon.serial.core.ReconnectPolicy;
import com.temon.serial.core.SerialError;
import com.temon.serial.core.SerialException;
import com.temon.serial.core.SerialFraming;
import com.temon.serial.core.SerialConfig;
import com.temon.serial.core.SerialDefaults;
import com.temon.serial.core.SerialManager;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Easy, singleton entry for "open once, use everywhere".
 *
 * <p>Defaults: 8N1, no parity, idle-gap framing, auto reconnection,
 * and callbacks on main thread.</p>
 */
public final class EasySerial {
    public static final int OPEN_OK = 0;
    public static final int OPEN_NO_PERMISSION = -1;
    public static final int OPEN_UNKNOWN_ERROR = -2;
    public static final int OPEN_INVALID_PARAM = -3;
    private static final int DEFAULT_IDLE_GAP_MS = SerialDefaults.IDLE_GAP_MS;
    private static final int DEFAULT_MAX_FRAME_LEN = SerialDefaults.MAX_FRAME_LENGTH;
    private static final long DEFAULT_RECONNECT_INITIAL_MS = SerialDefaults.RECONNECT_INITIAL_MS;
    private static final long DEFAULT_RECONNECT_MAX_MS = SerialDefaults.RECONNECT_MAX_MS;
    private static final double DEFAULT_RECONNECT_MULTIPLIER = SerialDefaults.RECONNECT_MULTIPLIER;
    private static final int DEFAULT_SEND_INTERVAL_MS = SerialDefaults.SEND_INTERVAL_MS;

    private final SerialManager manager;
    private final AtomicBoolean defaultsInited = new AtomicBoolean(false);
    private final AtomicBoolean errorHooked = new AtomicBoolean(false);

    private final ConcurrentHashMap<String, SerialManager.OnFrameListener> frameListenerMap =
            new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<OnDataReceivedListener> dataListeners =
            new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<OnDataSendListener> sendListeners =
            new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<OnErrorListener> errorListeners =
            new CopyOnWriteArrayList<>();

    private static final class Holder {
        private static final EasySerial INSTANCE = new EasySerial(new SerialManager());
    }

    public static EasySerial instance() {
        return Holder.INSTANCE;
    }

    private EasySerial(SerialManager manager) {
        if (manager == null) {
            throw new IllegalArgumentException("manager == null");
        }
        this.manager = manager;
    }

    // ---- Static proxy (singleton shortcuts) ----
    public static int open(String port, int baudRate) {
        return instance().openInternal(port, baudRate);
    }

    public static void send(String port, byte[] data) throws SerialException {
        instance().sendInternal(port, data);
    }

    public static void onDataReceived(OnDataReceivedListener listener) {
        instance().onDataReceivedInternal(listener);
    }

    public static void onError(OnErrorListener listener) {
        instance().onErrorInternal(listener);
    }

    public static void onDataSend(OnDataSendListener listener) {
        instance().onDataSendInternal(listener);
    }

    public static void removeOnDataReceived(OnDataReceivedListener listener) {
        instance().removeOnDataReceivedInternal(listener);
    }

    public static void clearOnDataReceived() {
        instance().clearOnDataReceivedInternal();
    }

    public static void removeOnDataSend(OnDataSendListener listener) {
        instance().removeOnDataSendInternal(listener);
    }

    public static void clearOnDataSend() {
        instance().clearOnDataSendInternal();
    }

    public static void removeOnError(OnErrorListener listener) {
        instance().removeOnErrorInternal(listener);
    }

    public static void clearOnError() {
        instance().clearOnErrorInternal();
    }

    public static void close(String port) {
        instance().closeInternal(port);
    }

    public static void closeAll() {
        instance().closeAllInternal();
    }

    public interface OnDataReceivedListener {
        /**
         * A complete frame (best-effort idle-gap framing).
         */
        void onDataReceived(String port, byte[] data, int length);
    }

    public interface OnDataSendListener {
        /**
         * Sent data (not copied). Do not modify the byte[].
         */
        void onDataSend(String port, byte[] data, int length);
    }

    public interface OnErrorListener {
        /**
         * Friendly error with optional throwable.
         */
        void onError(String port, SerialError error, String message, Throwable throwable);
    }

    /**
     * Open a serial port once for the whole app.
     */
    private int openInternal(String port, int baudRate) {
        if (port == null || port.trim().isEmpty()) {
            return OPEN_INVALID_PARAM;
        }
        if (baudRate <= 0) {
            return OPEN_INVALID_PARAM;
        }
        try {
            ensureDefaults();
            SerialConfig config = new SerialConfig.Builder()
                    .port(port)
                    .baudRate(baudRate)
                    .sendIntervalMs(DEFAULT_SEND_INTERVAL_MS)
                    .build();
            manager.open(config, SerialFraming.idleGap(
                    DEFAULT_IDLE_GAP_MS,
                    DEFAULT_MAX_FRAME_LEN
            ));
            ensureFrameListener(port);
            return OPEN_OK;
        } catch (SerialException e) {
            SerialError error = e.getError();
            if (error == SerialError.PERMISSION_DENIED) {
                return OPEN_NO_PERMISSION;
            }
            if (error == SerialError.INVALID_PARAMETER) {
                return OPEN_INVALID_PARAM;
            }
            return OPEN_UNKNOWN_ERROR;
        } catch (Throwable t) {
            return OPEN_UNKNOWN_ERROR;
        }
    }

    /**
     * Send raw bytes to a specific port.
     */
    private void sendInternal(String port, byte[] data) throws SerialException {
        if (port == null || port.trim().isEmpty()) {
            throw new SerialException(SerialError.INVALID_PARAMETER, "port cannot be empty");
        }
        manager.sendBytes(port, data);
        dispatchSendListeners(port, data);
    }

    private void onDataReceivedInternal(OnDataReceivedListener listener) {
        if (listener != null) {
            dataListeners.addIfAbsent(listener);
        }
    }

    private void onErrorInternal(OnErrorListener listener) {
        if (listener != null) {
            errorListeners.addIfAbsent(listener);
        }
    }

    private void onDataSendInternal(OnDataSendListener listener) {
        if (listener != null) {
            sendListeners.addIfAbsent(listener);
        }
    }

    private void removeOnDataReceivedInternal(OnDataReceivedListener listener) {
        if (listener != null) {
            dataListeners.remove(listener);
        }
    }

    private void clearOnDataReceivedInternal() {
        dataListeners.clear();
    }

    private void removeOnDataSendInternal(OnDataSendListener listener) {
        if (listener != null) {
            sendListeners.remove(listener);
        }
    }

    private void clearOnDataSendInternal() {
        sendListeners.clear();
    }

    private void removeOnErrorInternal(OnErrorListener listener) {
        if (listener != null) {
            errorListeners.remove(listener);
        }
    }

    private void clearOnErrorInternal() {
        errorListeners.clear();
    }

    private void closeInternal(String port) {
        if (port == null || port.trim().isEmpty()) {
            return;
        }
        manager.close(port);
        frameListenerMap.remove(port);
    }

    private void closeAllInternal() {
        manager.closeAll();
        frameListenerMap.clear();
    }

    private void ensureDefaults() {
        if (defaultsInited.compareAndSet(false, true)) {
            manager.setCallbackDispatcher(Dispatchers.mainThread());
            manager.setDefaultFrameDecoder(SerialFraming.idleGap(
                    DEFAULT_IDLE_GAP_MS,
                    DEFAULT_MAX_FRAME_LEN
            ));
            manager.setDefaultReconnectPolicy(new ReconnectPolicy.ExponentialBackoff(
                    0,
                    DEFAULT_RECONNECT_INITIAL_MS,
                    DEFAULT_RECONNECT_MULTIPLIER,
                    DEFAULT_RECONNECT_MAX_MS
            ));
        }
        if (errorHooked.compareAndSet(false, true)) {
            manager.addErrorListener(new SerialManager.OnErrorListener() {
                @Override
                public void onError(String port, SerialError error, String message, Throwable throwable) {
                    SerialError serialError = error != null ? error : toSerialError(throwable);
                    for (OnErrorListener l : errorListeners) {
                        try {
                            l.onError(port, serialError, message, throwable);
                        } catch (Throwable ignored) {
                            // Don't let listener exceptions break the system
                        }
                    }
                }
            });
        }
    }

    private void ensureFrameListener(final String port) {
        SerialManager.OnFrameListener listener = frameListenerMap.get(port);
        if (listener == null) {
            SerialManager.OnFrameListener newListener = new SerialManager.OnFrameListener() {
                @Override
                public void onFrame(byte[] frame, int length) {
                    for (OnDataReceivedListener l : dataListeners) {
                        try {
                            l.onDataReceived(port, frame, length);
                        } catch (Throwable ignored) {
                            // Isolate listener failures from IO threads.
                        }
                    }
                }
            };
            SerialManager.OnFrameListener existing = frameListenerMap.putIfAbsent(port, newListener);
            listener = existing != null ? existing : newListener;
        }
        manager.addFrameListener(port, listener);
    }

    private void dispatchSendListeners(String port, byte[] data) {
        if (sendListeners.isEmpty()) return;
        final int length = data != null ? data.length : 0;
        for (OnDataSendListener listener : sendListeners) {
            try {
                listener.onDataSend(port, data, length);
            } catch (Throwable ignored) {
                // Don't let listener exceptions break the system
            }
        }
    }

    private static SerialError toSerialError(Throwable t) {
        if (t instanceof SerialException) {
            return ((SerialException) t).getError();
        }
        if (t instanceof IOException) {
            return SerialError.IO_ERROR;
        }
        return SerialError.OPEN_FAILED;
    }
}

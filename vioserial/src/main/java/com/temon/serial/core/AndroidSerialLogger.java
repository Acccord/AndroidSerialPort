package com.temon.serial.core;

import android.util.Log;

import com.temon.serial.codec.HexCodec;

/**
 * Android Log-based implementation of SerialLogger.
 * 
 * <p>Logs to Android Logcat with tag "SerialPort" and appropriate log levels.</p>
 */
public class AndroidSerialLogger implements SerialLogger {
    private static final String TAG = "SerialPort";
    private final boolean enabled;
    private final boolean logRawBytes;
    private final boolean logFrames;
    private final boolean logStateChanges;
    private final boolean logErrors;
    private final boolean logInfo;

    public AndroidSerialLogger() {
        this(true, true, true, true, true, true);
    }

    public AndroidSerialLogger(boolean enabled) {
        this(enabled, enabled, enabled, enabled, enabled, enabled);
    }

    public AndroidSerialLogger(boolean enabled, boolean logRawBytes, boolean logFrames,
                               boolean logStateChanges, boolean logErrors, boolean logInfo) {
        this.enabled = enabled;
        this.logRawBytes = logRawBytes;
        this.logFrames = logFrames;
        this.logStateChanges = logStateChanges;
        this.logErrors = logErrors;
        this.logInfo = logInfo;
    }

    @Override
    public void logRxBytes(String port, byte[] data, int length) {
        if (!enabled || !logRawBytes) return;
        String hex = HexCodec.encode(data, 0, length);
        Log.d(TAG, String.format("[%s] RX: %s", port, hex));
    }

    @Override
    public void logTxBytes(String port, byte[] data, int length) {
        if (!enabled || !logRawBytes) return;
        String hex = HexCodec.encode(data, 0, length);
        Log.d(TAG, String.format("[%s] TX: %s", port, hex));
    }

    @Override
    public void logFrame(String port, byte[] frame, int length) {
        if (!enabled || !logFrames) return;
        String hex = HexCodec.encode(frame, 0, length);
        Log.d(TAG, String.format("[%s] Frame: %s", port, hex));
    }

    @Override
    public void logStateChange(String port, SerialConnection.State oldState, SerialConnection.State newState) {
        if (!enabled || !logStateChanges) return;
        Log.i(TAG, String.format("[%s] State: %s -> %s", port, oldState, newState));
    }

    @Override
    public void logError(String port, String error, Throwable throwable) {
        if (!enabled || !logErrors) return;
        if (throwable != null) {
            Log.e(TAG, String.format("[%s] Error: %s", port, error), throwable);
        } else {
            Log.e(TAG, String.format("[%s] Error: %s", port, error));
        }
    }

    @Override
    public void logInfo(String port, String message) {
        if (!enabled || !logInfo) return;
        Log.i(TAG, String.format("[%s] %s", port, message));
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}

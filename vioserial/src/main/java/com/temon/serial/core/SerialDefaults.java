package com.temon.serial.core;

/**
 * Centralized defaults for serial communication.
 */
public final class SerialDefaults {
    private SerialDefaults() {
    }

    // SerialConfig defaults
    public static final int READ_TIMEOUT_MS = 1000;
    public static final int DEVICE_CHECK_INTERVAL_MS = 5000;
    public static final int READ_BUFFER_SIZE = 1024;
    public static final int SEND_INTERVAL_MS = 300;

    // EasySerial defaults
    public static final int IDLE_GAP_MS = 50;
    public static final int MAX_FRAME_LENGTH = 2048;
    public static final long RECONNECT_INITIAL_MS = 500;
    public static final long RECONNECT_MAX_MS = 30000;
    public static final double RECONNECT_MULTIPLIER = 2.0;
}

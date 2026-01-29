package com.temon.serial.core;

/**
 * Configurable logger for serial port communication diagnostics.
 * 
 * <p>Supports logging of raw data (Hex format), state changes, and errors
 * for 7×24 hour operation troubleshooting.</p>
 */
public interface SerialLogger {
    /**
     * Log raw bytes received from serial port (Hex format).
     * 
     * @param port Serial port path
     * @param data Raw bytes received
     * @param length Number of bytes
     */
    void logRxBytes(String port, byte[] data, int length);

    /**
     * Log raw bytes sent to serial port (Hex format).
     * 
     * @param port Serial port path
     * @param data Raw bytes sent
     * @param length Number of bytes
     */
    void logTxBytes(String port, byte[] data, int length);

    /**
     * Log a complete frame decoded from raw bytes (Hex format).
     * 
     * @param port Serial port path
     * @param frame Complete frame bytes
     * @param length Frame length
     */
    void logFrame(String port, byte[] frame, int length);

    /**
     * Log state change.
     * 
     * @param port Serial port path
     * @param oldState Previous state
     * @param newState New state
     */
    void logStateChange(String port, SerialConnection.State oldState, SerialConnection.State newState);

    /**
     * Log error/exception.
     * 
     * @param port Serial port path
     * @param error Error message
     * @param throwable Exception (may be null)
     */
    void logError(String port, String error, Throwable throwable);

    /**
     * Log informational message.
     * 
     * @param port Serial port path
     * @param message Message
     */
    void logInfo(String port, String message);

    /**
     * Check if logging is enabled.
     * 
     * @return true if logging is enabled
     */
    boolean isEnabled();

    /**
     * No-op logger implementation (disabled).
     */
    SerialLogger NO_OP = new SerialLogger() {
        @Override
        public void logRxBytes(String port, byte[] data, int length) {}

        @Override
        public void logTxBytes(String port, byte[] data, int length) {}

        @Override
        public void logFrame(String port, byte[] frame, int length) {}

        @Override
        public void logStateChange(String port, SerialConnection.State oldState, SerialConnection.State newState) {}

        @Override
        public void logError(String port, String error, Throwable throwable) {}

        @Override
        public void logInfo(String port, String message) {}

        @Override
        public boolean isEnabled() {
            return false;
        }
    };
}

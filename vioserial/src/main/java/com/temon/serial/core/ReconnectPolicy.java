package com.temon.serial.core;

/**
 * Policy for automatic reconnection when serial port connection fails.
 * 
 * <p>This is an optional feature. By default, reconnection is disabled.
 * Applications should implement this interface to enable automatic reconnection.</p>
 */
public interface ReconnectPolicy {
    /**
     * Determine if reconnection should be attempted.
     * 
     * @param attemptCount Current attempt count (1-based)
     * @param lastError Last error that occurred
     * @return true if reconnection should be attempted, false to stop
     */
    boolean shouldReconnect(int attemptCount, Throwable lastError);

    /**
     * Get delay in milliseconds before next reconnection attempt.
     * 
     * <p>This can implement exponential backoff or fixed delay strategies.</p>
     * 
     * @param attemptCount Current attempt count (1-based)
     * @return Delay in milliseconds
     */
    long getReconnectDelayMs(int attemptCount);

    /**
     * Called when reconnection succeeds.
     * 
     * @param attemptCount Number of attempts made before success
     */
    void onReconnectSuccess(int attemptCount);

    /**
     * Called when reconnection fails after all attempts.
     * 
     * @param attemptCount Total number of attempts made
     * @param lastError Last error that occurred
     */
    void onReconnectFailed(int attemptCount, Throwable lastError);

    /**
     * Default implementation: no reconnection (disabled).
     */
    ReconnectPolicy NONE = new ReconnectPolicy() {
        @Override
        public boolean shouldReconnect(int attemptCount, Throwable lastError) {
            return false;
        }

        @Override
        public long getReconnectDelayMs(int attemptCount) {
            return 0;
        }

        @Override
        public void onReconnectSuccess(int attemptCount) {}

        @Override
        public void onReconnectFailed(int attemptCount, Throwable lastError) {}
    };

    /**
     * Simple exponential backoff reconnection policy.
     */
    class ExponentialBackoff implements ReconnectPolicy {
        private final int maxAttempts;
        private final long initialDelayMs;
        private final double backoffMultiplier;
        private final long maxDelayMs;

        /**
         * Create exponential backoff policy.
         * 
         * @param maxAttempts Maximum number of reconnection attempts (0 = unlimited)
         * @param initialDelayMs Initial delay in milliseconds
         * @param backoffMultiplier Multiplier for each attempt (e.g., 2.0 for doubling)
         * @param maxDelayMs Maximum delay cap in milliseconds
         */
        public ExponentialBackoff(int maxAttempts, long initialDelayMs, double backoffMultiplier, long maxDelayMs) {
            this.maxAttempts = maxAttempts;
            this.initialDelayMs = initialDelayMs;
            this.backoffMultiplier = backoffMultiplier;
            this.maxDelayMs = maxDelayMs;
        }

        @Override
        public boolean shouldReconnect(int attemptCount, Throwable lastError) {
            if (maxAttempts > 0 && attemptCount >= maxAttempts) {
                return false;
            }
            // Don't reconnect on permission errors
            if (lastError instanceof SerialException) {
                SerialException se = (SerialException) lastError;
                if (se.getError() == SerialError.PERMISSION_DENIED) {
                    return false;
                }
            }
            if (lastError instanceof SecurityException) {
                return false;
            }
            return true;
        }

        @Override
        public long getReconnectDelayMs(int attemptCount) {
            if (attemptCount <= 1) {
                return initialDelayMs;
            }
            long delay = (long) (initialDelayMs * Math.pow(backoffMultiplier, attemptCount - 1));
            return Math.min(delay, maxDelayMs);
        }

        @Override
        public void onReconnectSuccess(int attemptCount) {
            // Default: no-op
        }

        @Override
        public void onReconnectFailed(int attemptCount, Throwable lastError) {
            // Default: no-op
        }
    }
}

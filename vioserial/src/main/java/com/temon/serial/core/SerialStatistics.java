package com.temon.serial.core;

import android.os.SystemClock;

/**
 * Statistics and performance metrics for serial port communication.
 * 
 * <p>Useful for monitoring, diagnostics, and performance optimization
 * in automotive and industrial applications.</p>
 */

public final class SerialStatistics {
    private volatile long bytesReceived = 0;
    private volatile long bytesSent = 0;
    private volatile long framesReceived = 0;
    private volatile long framesSent = 0;
    private volatile long readErrors = 0;
    private volatile long writeErrors = 0;
    private volatile long reconnectCount = 0;
    
    private volatile long lastReceiveTime = 0;
    private volatile long lastSendTime = 0;
    private volatile long sessionStartTime = 0;
    
    // Throughput calculation
    private volatile long lastThroughputResetTime = 0;
    private volatile long bytesReceivedSinceReset = 0;
    private volatile long bytesSentSinceReset = 0;

    public SerialStatistics() {
        reset();
    }

    /**
     * Reset all statistics.
     */
    public synchronized void reset() {
        bytesReceived = 0;
        bytesSent = 0;
        framesReceived = 0;
        framesSent = 0;
        readErrors = 0;
        writeErrors = 0;
        reconnectCount = 0;
        lastReceiveTime = 0;
        lastSendTime = 0;
        sessionStartTime = SystemClock.elapsedRealtime();
        lastThroughputResetTime = sessionStartTime;
        bytesReceivedSinceReset = 0;
        bytesSentSinceReset = 0;
    }

    public synchronized void onBytesReceived(int count) {
        bytesReceived += count;
        bytesReceivedSinceReset += count;
        lastReceiveTime = SystemClock.elapsedRealtime();
    }

    public synchronized void onBytesSent(int count) {
        bytesSent += count;
        bytesSentSinceReset += count;
        lastSendTime = SystemClock.elapsedRealtime();
    }

    public synchronized void onFrameReceived() {
        framesReceived++;
    }

    public synchronized void onFrameSent() {
        framesSent++;
    }

    public synchronized void onReadError() {
        readErrors++;
    }

    public synchronized void onWriteError() {
        writeErrors++;
    }

    public synchronized void onReconnect() {
        reconnectCount++;
    }

    /**
     * Get total bytes received since session start.
     */
    public long getBytesReceived() {
        return bytesReceived;
    }

    /**
     * Get total bytes sent since session start.
     */
    public long getBytesSent() {
        return bytesSent;
    }

    /**
     * Get total frames received since session start.
     */
    public long getFramesReceived() {
        return framesReceived;
    }

    /**
     * Get total frames sent since session start.
     */
    public long getFramesSent() {
        return framesSent;
    }

    /**
     * Get total read errors since session start.
     */
    public long getReadErrors() {
        return readErrors;
    }

    /**
     * Get total write errors since session start.
     */
    public long getWriteErrors() {
        return writeErrors;
    }

    /**
     * Get total reconnection count since session start.
     */
    public long getReconnectCount() {
        return reconnectCount;
    }

    /**
     * Get session uptime in milliseconds.
     */
    public long getSessionUptimeMs() {
        if (sessionStartTime == 0) return 0;
        return SystemClock.elapsedRealtime() - sessionStartTime;
    }

    /**
     * Get receive throughput in bytes per second.
     * 
     * @return Throughput in B/s, or 0 if insufficient data
     */
    public double getReceiveThroughputBps() {
        long now = SystemClock.elapsedRealtime();
        long elapsed = now - lastThroughputResetTime;
        if (elapsed < 1000) return 0;  // Need at least 1 second of data
        return (bytesReceivedSinceReset * 1000.0) / elapsed;
    }

    /**
     * Get send throughput in bytes per second.
     * 
     * @return Throughput in B/s, or 0 if insufficient data
     */
    public double getSendThroughputBps() {
        long now = SystemClock.elapsedRealtime();
        long elapsed = now - lastThroughputResetTime;
        if (elapsed < 1000) return 0;  // Need at least 1 second of data
        return (bytesSentSinceReset * 1000.0) / elapsed;
    }

    /**
     * Get error rate (errors per second).
     */
    public double getErrorRate() {
        long uptime = getSessionUptimeMs();
        if (uptime == 0) return 0;
        return ((readErrors + writeErrors) * 1000.0) / uptime;
    }

    /**
     * Get time since last receive in milliseconds.
     * Returns -1 if no data has been received.
     */
    public long getTimeSinceLastReceiveMs() {
        if (lastReceiveTime == 0) return -1;
        return SystemClock.elapsedRealtime() - lastReceiveTime;
    }

    /**
     * Get time since last send in milliseconds.
     * Returns -1 if no data has been sent.
     */
    public long getTimeSinceLastSendMs() {
        if (lastSendTime == 0) return -1;
        return SystemClock.elapsedRealtime() - lastSendTime;
    }

    /**
     * Reset throughput calculation baseline.
     */
    public synchronized void resetThroughput() {
        lastThroughputResetTime = SystemClock.elapsedRealtime();
        bytesReceivedSinceReset = 0;
        bytesSentSinceReset = 0;
    }

    @Override
    public String toString() {
        return String.format(
            "SerialStatistics{bytesRx=%d, bytesTx=%d, framesRx=%d, framesTx=%d, " +
            "errorsRx=%d, errorsTx=%d, reconnects=%d, uptime=%dms, " +
            "rxThroughput=%.2f B/s, txThroughput=%.2f B/s, errorRate=%.4f/s}",
            bytesReceived, bytesSent, framesReceived, framesSent,
            readErrors, writeErrors, reconnectCount, getSessionUptimeMs(),
            getReceiveThroughputBps(), getSendThroughputBps(), getErrorRate()
        );
    }
}

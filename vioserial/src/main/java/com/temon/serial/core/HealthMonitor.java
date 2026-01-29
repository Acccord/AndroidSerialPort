package com.temon.serial.core;

/**
 * Health monitoring and self-diagnosis for serial port connections.
 * 
 * <p>Designed for automotive and industrial applications requiring
 * continuous health monitoring and predictive failure detection.</p>
 */
public final class HealthMonitor {
    public enum HealthStatus {
        HEALTHY,        // All systems normal
        WARNING,        // Degraded performance, but functional
        CRITICAL,       // Significant issues, may fail soon
        FAILED          // Connection failed
    }
    
    private final SerialConnection connection;
    private final SerialStatistics statistics;
    private final long maxIdleTimeMs;  // Max time without data before warning
    private final double maxErrorRate;  // Max error rate before warning
    private final long maxReconnectCount;  // Max reconnects before warning
    
    public HealthMonitor(SerialConnection connection, SerialStatistics statistics,
                        long maxIdleTimeMs, double maxErrorRate, long maxReconnectCount) {
        this.connection = connection;
        this.statistics = statistics;
        this.maxIdleTimeMs = maxIdleTimeMs;
        this.maxErrorRate = maxErrorRate;
        this.maxReconnectCount = maxReconnectCount;
    }
    
    /**
     * Perform health check and return current status.
     */
    public HealthStatus checkHealth() {
        SerialConnection.State state = connection.getState();
        
        // Failed if in ERROR or CLOSED state
        if (state == SerialConnection.State.ERROR) {
            return HealthStatus.FAILED;
        }
        if (state == SerialConnection.State.CLOSED) {
            return HealthStatus.FAILED;
        }
        
        // Check idle time
        long idleTime = statistics.getTimeSinceLastReceiveMs();
        if (idleTime > 0 && idleTime > maxIdleTimeMs * 2) {
            return HealthStatus.CRITICAL;  // No data for too long
        }
        if (idleTime > 0 && idleTime > maxIdleTimeMs) {
            return HealthStatus.WARNING;  // Approaching idle threshold
        }
        
        // Check error rate
        double errorRate = statistics.getErrorRate();
        if (errorRate > maxErrorRate * 2) {
            return HealthStatus.CRITICAL;  // Very high error rate
        }
        if (errorRate > maxErrorRate) {
            return HealthStatus.WARNING;  // Elevated error rate
        }
        
        // Check reconnection count
        long reconnectCount = statistics.getReconnectCount();
        if (reconnectCount > maxReconnectCount * 2) {
            return HealthStatus.CRITICAL;  // Too many reconnects
        }
        if (reconnectCount > maxReconnectCount) {
            return HealthStatus.WARNING;  // Frequent reconnects
        }
        
        // Check if connection is open
        if (state != SerialConnection.State.OPEN) {
            return HealthStatus.WARNING;  // Not fully open
        }
        
        return HealthStatus.HEALTHY;
    }
    
    /**
     * Get detailed health diagnosis report.
     */
    public String getDiagnosis() {
        HealthStatus status = checkHealth();
        StringBuilder report = new StringBuilder();
        report.append("Health Status: ").append(status).append("\n");
        
        SerialConnection.State state = connection.getState();
        report.append("Connection State: ").append(state).append("\n");
        
        long uptime = statistics.getSessionUptimeMs();
        report.append("Uptime: ").append(uptime / 1000).append(" seconds\n");
        
        long idleTime = statistics.getTimeSinceLastReceiveMs();
        if (idleTime > 0) {
            report.append("Time since last receive: ").append(idleTime).append(" ms\n");
        }
        
        double errorRate = statistics.getErrorRate();
        report.append("Error rate: ").append(String.format("%.4f", errorRate)).append(" errors/s\n");
        
        long reconnectCount = statistics.getReconnectCount();
        report.append("Reconnection count: ").append(reconnectCount).append("\n");
        
        double rxThroughput = statistics.getReceiveThroughputBps();
        double txThroughput = statistics.getSendThroughputBps();
        report.append("Receive throughput: ").append(String.format("%.2f", rxThroughput)).append(" B/s\n");
        report.append("Send throughput: ").append(String.format("%.2f", txThroughput)).append(" B/s\n");
        
        Throwable lastError = connection.getLastError();
        if (lastError != null) {
            report.append("Last error: ").append(lastError.getMessage()).append("\n");
        }
        
        // Add recommendations
        if (status == HealthStatus.WARNING || status == HealthStatus.CRITICAL) {
            report.append("\nRecommendations:\n");
            if (idleTime > maxIdleTimeMs) {
                report.append("- Check device connection and data flow\n");
            }
            if (errorRate > maxErrorRate) {
                report.append("- Investigate error sources (cable, interference, baud rate)\n");
            }
            if (reconnectCount > maxReconnectCount) {
                report.append("- Check device stability and power supply\n");
            }
        }
        
        return report.toString();
    }
    
    /**
     * Builder for HealthMonitor configuration.
     */
    public static final class Builder {
        private long maxIdleTimeMs = 30000;  // 30 seconds default
        private double maxErrorRate = 1.0;  // 1 error per second
        private long maxReconnectCount = 5;  // 5 reconnects
        
        public Builder maxIdleTimeMs(long maxIdleTimeMs) {
            this.maxIdleTimeMs = maxIdleTimeMs;
            return this;
        }
        
        public Builder maxErrorRate(double maxErrorRate) {
            this.maxErrorRate = maxErrorRate;
            return this;
        }
        
        public Builder maxReconnectCount(long maxReconnectCount) {
            this.maxReconnectCount = maxReconnectCount;
            return this;
        }
        
        public HealthMonitor build(SerialConnection connection, SerialStatistics statistics) {
            return new HealthMonitor(connection, statistics, maxIdleTimeMs, maxErrorRate, maxReconnectCount);
        }
    }
}

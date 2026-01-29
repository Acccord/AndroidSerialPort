package com.temon.serial.core;

import java.nio.charset.Charset;

public final class SerialConfig {
    public final String port;
    public final int baudRate;
    public final int stopBits;
    public final int dataBits;
    public final int parity;
    public final int flowCon;
    public final int flags;
    public final Charset textCharset;
    public final int sendIntervalMs;
    public final int readTimeoutMs;
    public final int deviceCheckIntervalMs;
    public final boolean useNioMode;  // Use NIO Selector for better timeout precision
    public final int readBufferSize;  // Read buffer size (adaptive if <= 0)
    public final PermissionStrategy permissionStrategy;

    private SerialConfig(Builder b) {
        this.port = b.port;
        this.baudRate = b.baudRate;
        this.stopBits = b.stopBits;
        this.dataBits = b.dataBits;
        this.parity = b.parity;
        this.flowCon = b.flowCon;
        this.flags = b.flags;
        this.textCharset = b.textCharset;
        this.sendIntervalMs = b.sendIntervalMs;
        this.readTimeoutMs = b.readTimeoutMs;
        this.deviceCheckIntervalMs = b.deviceCheckIntervalMs;
        this.useNioMode = b.useNioMode;
        this.readBufferSize = b.readBufferSize > 0 ? b.readBufferSize : SerialDefaults.READ_BUFFER_SIZE;
        this.permissionStrategy = b.permissionStrategy;
    }

    public static final class Builder {
        private String port;
        private int baudRate;
        private int stopBits = 1;
        private int dataBits = 8;
        private int parity = 0;
        private int flowCon = 0;
        private int flags = 0;
        private Charset textCharset = Charset.forName("UTF-8");
        private int sendIntervalMs = 0;
        private int readTimeoutMs = SerialDefaults.READ_TIMEOUT_MS;  // Default timeout for device checks
        private int deviceCheckIntervalMs = SerialDefaults.DEVICE_CHECK_INTERVAL_MS;  // Default: check every 5 seconds
        private boolean useNioMode = false;  // Default: use traditional blocking I/O
        private int readBufferSize = 0;  // 0 = use default, >0 = custom size
        private PermissionStrategy permissionStrategy;

        public Builder port(String port) {
            this.port = port;
            return this;
        }

        public Builder baudRate(int baudRate) {
            this.baudRate = baudRate;
            return this;
        }

        public Builder stopBits(int stopBits) {
            this.stopBits = stopBits;
            return this;
        }

        public Builder dataBits(int dataBits) {
            this.dataBits = dataBits;
            return this;
        }

        public Builder parity(int parity) {
            this.parity = parity;
            return this;
        }

        public Builder flowCon(int flowCon) {
            this.flowCon = flowCon;
            return this;
        }

        public Builder flags(int flags) {
            this.flags = flags;
            return this;
        }

        public Builder textCharset(Charset charset) {
            if (charset == null) throw new IllegalArgumentException("charset == null");
            this.textCharset = charset;
            return this;
        }

        /**
         * Minimum interval between two writes. Default: 0.
         */
        public Builder sendIntervalMs(int sendIntervalMs) {
            this.sendIntervalMs = Math.max(0, sendIntervalMs);
            return this;
        }

        /**
         * Read timeout in milliseconds. Default: 1000 (1s).
         * 
         * <p>If > 0, read operations will timeout after the specified milliseconds.
         * This enables non-blocking reads with timeout control.</p>
         * 
         * <p>If 0, read operations will block indefinitely until data is available.</p>
         */
        public Builder readTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = Math.max(0, readTimeoutMs);
            return this;
        }

        /**
         * Device online check interval in milliseconds. Default: 5000 (5 seconds).
         * 
         * <p>Periodically checks if the device is still connected. If device is detected
         * as disconnected, the connection will transition to ERROR state.</p>
         * 
         * <p>Set to 0 to disable device checking (not recommended for long-running applications).</p>
         */
        public Builder deviceCheckIntervalMs(int deviceCheckIntervalMs) {
            this.deviceCheckIntervalMs = Math.max(0, deviceCheckIntervalMs);
            return this;
        }

        /**
         * Enable NIO Selector mode for millisecond-precision timeout and better interrupt response.
         * 
         * <p>When enabled, uses Java NIO Selector instead of traditional blocking I/O.
         * This provides:</p>
         * <ul>
         *   <li>Millisecond-precision timeout (vs 0.1s precision with VMIN/VTIME)</li>
         *   <li>Better interrupt response</li>
         *   <li>More efficient I/O model</li>
         * </ul>
         * 
         * <p>Recommended for automotive and high-reliability applications.</p>
         * 
         * <p>Default: false (uses traditional blocking I/O with VMIN/VTIME)</p>
         */
        public Builder useNioMode(boolean useNioMode) {
            this.useNioMode = useNioMode;
            return this;
        }

        /**
         * Set read buffer size in bytes. Default: 1024.
         * 
         * <p>Larger buffers improve throughput but increase memory usage.
         * For high-speed applications (e.g., automotive CAN bus), consider 4096-8192.</p>
         * 
         * <p>Set to 0 to use default (1024 bytes).</p>
         */
        public Builder readBufferSize(int readBufferSize) {
            this.readBufferSize = Math.max(0, readBufferSize);
            return this;
        }

        /**
         * Set permission strategy for opening device nodes.
         *
         * <p>If null, uses the default strategy (no root operations).</p>
         */
        public Builder permissionStrategy(PermissionStrategy permissionStrategy) {
            this.permissionStrategy = permissionStrategy;
            return this;
        }

        public SerialConfig build() {
            if (port == null || port.trim().isEmpty()) {
                throw new IllegalArgumentException("port cannot be empty");
            }
            if (baudRate <= 0) {
                throw new IllegalArgumentException("baudRate must be > 0");
            }
            return new SerialConfig(this);
        }
    }
}



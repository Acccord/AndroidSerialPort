package com.temon.serial.core;

/**
 * Decodes a byte stream into frames.
 *
 * <p>Use {@link SerialFraming} to obtain built-in decoders. Internal implementations live under
 * {@code com.temon.serial.internal} and are not part of the public API.</p>
 *
 * <p><b>Thread Safety:</b> Implementations MUST be thread-confined. All methods are called
 * exclusively from the read thread of {@link SerialConnection}. This means:
 * <ul>
 *   <li>{@link #feed(byte[], int, int, FrameCallback)} is called from the read thread only</li>
 *   <li>{@link #reset()} is called from the read thread or during open/close synchronization</li>
 *   <li>No concurrent calls to feed() or reset() will occur</li>
 *   <li>Implementations do NOT need to be thread-safe</li>
 * </ul>
 *
 * <p><b>Call Timing:</b>
 * <ul>
 *   <li>{@code feed()} is called whenever raw bytes are received from the serial port</li>
 *   <li>{@code reset()} is called when the connection is opened or closed</li>
 *   <li>After reset(), the decoder should clear any internal buffered state</li>
 * </ul>
 *
 * <p><b>Frame Emission:</b> The decoder may emit 0 to N frames per feed() call, depending on
 * the framing strategy (e.g., delimiter-based may emit multiple frames, length-field emits
 * one frame per complete packet).
 */
public interface FrameDecoder {

    /**
     * Callback for emitting complete frames.
     */
    interface FrameCallback {
        /**
         * Called when a complete frame is decoded.
         * 
         * @param frameBytes Frame bytes (may be a copy or reference to internal buffer)
         * @param length Number of bytes in the frame
         */
        void onFrame(byte[] frameBytes, int length);
    }

    /**
     * Feed bytes into decoder. Decoder may emit 0..N frames via callback.
     * 
     * <p>Called from the read thread only. No concurrent calls.</p>
     * 
     * @param data Byte array containing data
     * @param offset Start offset in data array
     * @param length Number of bytes to process
     * @param callback Callback to emit complete frames
     */
    void feed(byte[] data, int offset, int length, FrameCallback callback);

    /**
     * Reset internal state. Called on open/close.
     * 
     * <p>Called when the connection is opened or closed. The decoder should clear
     * any internal buffered state. After reset(), the next feed() call starts fresh.</p>
     * 
     * <p>Called from the read thread or during open/close synchronization. No concurrent calls.</p>
     */
    void reset();
}



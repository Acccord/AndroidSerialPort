package com.temon.serial.core;

/**
 * Optional extension: decoder may hold a partial frame which can be flushed (e.g. idle-gap framing).
 */
public interface FlushableFrameDecoder extends FrameDecoder {
    /**
     * Emit any buffered frame if present.
     */
    void flush(FrameCallback callback);
}



package com.temon.serial.core;

public interface SerialListener {
    void onOpen();

    void onClose();

    /**
     * Raw bytes chunk from the underlying stream (not framed).
     */
    void onBytes(byte[] data, int length);

    /**
     * A complete frame emitted by {@link FrameDecoder}.
     */
    void onFrame(byte[] frame, int length);

    void onError(Throwable t);
}



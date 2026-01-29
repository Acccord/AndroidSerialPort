package com.temon.serial.core;

/**
 * Convenience adapter to avoid implementing all methods.
 */
public abstract class SerialListenerAdapter implements SerialListener {
    @Override
    public void onOpen() {
    }

    @Override
    public void onClose() {
    }

    @Override
    public void onBytes(byte[] data, int length) {
    }

    @Override
    public void onFrame(byte[] frame, int length) {
    }

    @Override
    public void onError(Throwable t) {
    }
}



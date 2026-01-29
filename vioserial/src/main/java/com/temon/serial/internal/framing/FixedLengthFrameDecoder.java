package com.temon.serial.internal.framing;

import com.temon.serial.core.FrameDecoder;

import java.util.Arrays;

/**
 * Emits a frame each time accumulated bytes reach a fixed length.
 */
public final class FixedLengthFrameDecoder implements FrameDecoder {
    private final int frameLength;

    private byte[] buf;
    private int size = 0;

    public FixedLengthFrameDecoder(int frameLength) {
        if (frameLength <= 0) {
            throw new IllegalArgumentException("frameLength must be > 0");
        }
        this.frameLength = frameLength;
        this.buf = new byte[Math.max(1024, frameLength)];
    }

    @Override
    public void feed(byte[] data, int offset, int length, FrameCallback callback) {
        if (length <= 0) return;
        ensureCapacity(size + length);
        System.arraycopy(data, offset, buf, size, length);
        size += length;

        while (size >= frameLength) {
            byte[] frame = Arrays.copyOfRange(buf, 0, frameLength);
            callback.onFrame(frame, frame.length);

            int remaining = size - frameLength;
            if (remaining > 0) {
                System.arraycopy(buf, frameLength, buf, 0, remaining);
            }
            size = remaining;
        }
    }

    @Override
    public void reset() {
        size = 0;
    }

    private void ensureCapacity(int desired) {
        if (buf.length >= desired) return;
        int newCap = buf.length;
        while (newCap < desired) newCap *= 2;
        buf = Arrays.copyOf(buf, newCap);
    }
}

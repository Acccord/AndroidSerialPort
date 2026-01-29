package com.temon.serial.internal.framing;

import com.temon.serial.core.FrameDecoder;

import java.util.Arrays;

/**
 * Length-field based framing.
 *
 * <p>Typical binary frame: [header...][len][payload...][crc...]
 * where {@code len} can mean:
 * - payload length
 * - payload+crc length
 * - entire frame length
 *
 * <p>This decoder is intentionally small but configurable enough for common devices.</p>
 */
public final class LengthFieldFrameDecoder implements FrameDecoder {

    public enum Endian {BIG, LITTLE}

    private final int lengthFieldOffset;
    private final int lengthFieldLength;
    private final Endian endian;
    private final int lengthAdjustment;
    private final int initialBytesToStrip;
    private final int maxFrameLength;

    private byte[] buf = new byte[1024];
    private int size = 0;

    public LengthFieldFrameDecoder(
            int lengthFieldOffset,
            int lengthFieldLength,
            Endian endian,
            int lengthAdjustment,
            int initialBytesToStrip,
            int maxFrameLength
    ) {
        if (lengthFieldOffset < 0) throw new IllegalArgumentException("lengthFieldOffset < 0");
        if (lengthFieldLength < 1 || lengthFieldLength > 4) throw new IllegalArgumentException("lengthFieldLength must be 1..4");
        if (endian == null) throw new IllegalArgumentException("endian == null");
        if (initialBytesToStrip < 0) throw new IllegalArgumentException("initialBytesToStrip < 0");
        if (maxFrameLength <= 0) throw new IllegalArgumentException("maxFrameLength must be > 0");
        this.lengthFieldOffset = lengthFieldOffset;
        this.lengthFieldLength = lengthFieldLength;
        this.endian = endian;
        this.lengthAdjustment = lengthAdjustment;
        this.initialBytesToStrip = initialBytesToStrip;
        this.maxFrameLength = maxFrameLength;
    }

    @Override
    public void feed(byte[] data, int offset, int length, FrameCallback callback) {
        if (length <= 0) return;
        ensureCapacity(size + length);
        System.arraycopy(data, offset, buf, size, length);
        size += length;

        // Try decode as many frames as possible.
        while (true) {
            int minHeader = lengthFieldOffset + lengthFieldLength;
            if (size < minHeader) return; // not enough to read length

            int fieldValue = readUnsignedInt(buf, lengthFieldOffset, lengthFieldLength, endian);
            long frameLenLong = (long) minHeader + (long) fieldValue + (long) lengthAdjustment;
            if (frameLenLong < 0 || frameLenLong > Integer.MAX_VALUE) {
                // corrupted stream; drop buffer
                reset();
                return;
            }
            int frameLength = (int) frameLenLong;

            if (frameLength <= 0 || frameLength > maxFrameLength) {
                // safety: corrupted length; drop buffer
                reset();
                return;
            }

            if (size < frameLength) return; // wait for more bytes

            int emitOffset = Math.min(initialBytesToStrip, frameLength);
            int emitLen = frameLength - emitOffset;
            if (emitLen > 0) {
                byte[] frame = Arrays.copyOfRange(buf, emitOffset, frameLength);
                callback.onFrame(frame, frame.length);
            }

            // consume frameLength
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

    private static int readUnsignedInt(byte[] b, int offset, int len, Endian endian) {
        int v = 0;
        if (endian == Endian.BIG) {
            for (int i = 0; i < len; i++) {
                v = (v << 8) | (b[offset + i] & 0xFF);
            }
        } else {
            for (int i = len - 1; i >= 0; i--) {
                v = (v << 8) | (b[offset + i] & 0xFF);
            }
        }
        return v;
    }
}

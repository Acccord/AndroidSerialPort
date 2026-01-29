package com.temon.serial.internal.framing;

import com.temon.serial.core.FrameDecoder;

import java.util.Arrays;

/**
 * Splits frames by a delimiter byte sequence (delimiter is included in output by default = false).
 */
public final class DelimiterFrameDecoder implements FrameDecoder {

    private final byte[] delimiter;
    private final boolean includeDelimiter;

    private byte[] buf = new byte[1024];
    private int size = 0;

    public DelimiterFrameDecoder(byte[] delimiter, boolean includeDelimiter) {
        if (delimiter == null || delimiter.length == 0) {
            throw new IllegalArgumentException("delimiter cannot be empty");
        }
        this.delimiter = Arrays.copyOf(delimiter, delimiter.length);
        this.includeDelimiter = includeDelimiter;
    }

    @Override
    public void feed(byte[] data, int offset, int length, FrameCallback callback) {
        if (length <= 0) return;
        ensureCapacity(size + length);
        System.arraycopy(data, offset, buf, size, length);
        size += length;

        int searchFrom = 0;
        while (true) {
            int idx = indexOf(buf, 0, size, delimiter, searchFrom);
            if (idx < 0) break;

            int frameEnd = includeDelimiter ? (idx + delimiter.length) : idx;
            if (frameEnd > 0) {
                byte[] frame = Arrays.copyOfRange(buf, 0, frameEnd);
                callback.onFrame(frame, frame.length);
            }

            // remove [0, idx + delimiter.length)
            int consumed = idx + delimiter.length;
            int remaining = size - consumed;
            if (remaining > 0) {
                System.arraycopy(buf, consumed, buf, 0, remaining);
            }
            size = remaining;
            searchFrom = 0;
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

    private static int indexOf(byte[] haystack, int hayOffset, int hayLen, byte[] needle, int from) {
        int max = hayLen - needle.length;
        for (int i = Math.max(from, hayOffset); i <= max; i++) {
            boolean match = true;
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    match = false;
                    break;
                }
            }
            if (match) return i;
        }
        return -1;
    }
}

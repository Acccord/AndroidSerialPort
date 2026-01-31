package com.temon.serial.internal.framing;

import android.util.Log;

import com.temon.serial.core.FrameDecoder;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Splits frames by a delimiter byte sequence (delimiter is included in output by default = false).
 */
public final class DelimiterFrameDecoder implements FrameDecoder {

    private static final String TAG = "DelimiterFrameDecoder";

    private final byte[] delimiter;
    private final boolean includeDelimiter;

    private static final int MAX_BUFFER_SIZE = 1024 * 1024; // 1 MB safety cap

    private byte[] buf = new byte[1024];
    private int size = 0;
    private final AtomicLong droppedCount = new AtomicLong(0);
    private final AtomicLong droppedBytes = new AtomicLong(0);

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
        if (size + length > MAX_BUFFER_SIZE) {
            // Safety: drop buffered data to avoid unbounded growth
            Log.w(TAG, "Buffer overflow, dropping buffered data. size=" + size + ", incoming=" + length);
            recordDrop(size + length);
            reset();
            return;
        }
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

    /**
     * Number of times buffered data was dropped due to safety limits.
     */
    public long getDroppedCount() {
        return droppedCount.get();
    }

    /**
     * Total bytes dropped due to safety limits.
     */
    public long getDroppedBytes() {
        return droppedBytes.get();
    }

    private void recordDrop(int bytes) {
        droppedCount.incrementAndGet();
        if (bytes > 0) {
            droppedBytes.addAndGet(bytes);
        }
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

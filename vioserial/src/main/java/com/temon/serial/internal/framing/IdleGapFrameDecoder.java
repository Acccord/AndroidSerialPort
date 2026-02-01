package com.temon.serial.internal.framing;

import android.os.SystemClock;

import com.temon.serial.core.FlushableFrameDecoder;

import java.util.Arrays;

/**
 * Frames are split by time gap between incoming bytes.
 *
 * <p>Behavior: if time gap between two feed() calls exceeds {@code idleGapMs},
 * the previous buffered bytes are emitted as a frame, then the new bytes start a new frame.</p>
 *
 * <p>Note: This decoder will only emit the last frame when either:
 * - a new chunk arrives after a gap, or
 * - caller invokes {@link #flush(FrameCallback)} (e.g. on close).</p>
 */
public final class IdleGapFrameDecoder implements FlushableFrameDecoder {
    private final long idleGapMs;
    private final int maxFrameLength;

    private byte[] buf = new byte[1024];
    private int size = 0;
    private long lastFeedUptimeMs = -1L;

    public IdleGapFrameDecoder(long idleGapMs, int maxFrameLength) {
        if (idleGapMs <= 0) throw new IllegalArgumentException("idleGapMs must be > 0");
        if (maxFrameLength <= 0) throw new IllegalArgumentException("maxFrameLength must be > 0");
        this.idleGapMs = idleGapMs;
        this.maxFrameLength = maxFrameLength;
    }

    public long getIdleGapMs() {
        return idleGapMs;
    }

    @Override
    public void feed(byte[] data, int offset, int length, FrameCallback callback) {
        if (length <= 0) return;
        long now = SystemClock.uptimeMillis();
        if (lastFeedUptimeMs > 0 && (now - lastFeedUptimeMs) >= idleGapMs) {
            flush(callback);
        }
        lastFeedUptimeMs = now;

        int desired = size + length;
        if (desired > maxFrameLength) {
            // corrupted/unbounded stream under idle framing; drop buffer to avoid OOM
            reset();
            return;
        }
        ensureCapacity(desired);
        System.arraycopy(data, offset, buf, size, length);
        size += length;
    }

    @Override
    public void flush(FrameCallback callback) {
        if (size <= 0) return;
        byte[] frame = Arrays.copyOf(buf, size);
        callback.onFrame(frame, frame.length);
        size = 0;
    }

    @Override
    public void reset() {
        size = 0;
        lastFeedUptimeMs = -1L;
    }

    private void ensureCapacity(int desired) {
        if (buf.length >= desired) return;
        int newCap = buf.length;
        while (newCap < desired) newCap *= 2;
        buf = Arrays.copyOf(buf, newCap);
    }
}

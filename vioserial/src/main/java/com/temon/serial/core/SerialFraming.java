package com.temon.serial.core;

import com.temon.serial.internal.framing.DelimiterFrameDecoder;
import com.temon.serial.internal.framing.FixedLengthFrameDecoder;
import com.temon.serial.internal.framing.IdleGapFrameDecoder;
import com.temon.serial.internal.framing.LengthFieldFrameDecoder;

/**
 * Built-in framing strategies (optional).
 *
 * <p>Core design: the library is protocol-agnostic. You choose a {@link FrameDecoder} only if you
 * need framed messages.</p>
 */
public final class SerialFraming {
    private SerialFraming() {
    }

    /** Common "line protocol" framing: CRLF (0x0D0A). */
    public static FrameDecoder crlf() {
        return new DelimiterFrameDecoder(new byte[]{0x0D, 0x0A}, false);
    }

    /** Split by custom delimiter bytes. */
    public static FrameDecoder delimiter(byte[] delimiter, boolean includeDelimiter) {
        return new DelimiterFrameDecoder(delimiter, includeDelimiter);
    }

    /** Fixed-size frames (binary protocols). */
    public static FrameDecoder fixedLength(int frameLength) {
        return new FixedLengthFrameDecoder(frameLength);
    }

    /**
     * Idle-gap framing: split frames by silence between bytes (best-effort legacy protocol support).
     */
    public static FrameDecoder idleGap(long idleGapMs, int maxFrameLength) {
        return new IdleGapFrameDecoder(idleGapMs, maxFrameLength);
    }

    /**
     * Length-field based framing (binary protocols).
     *
     * <p>frameLength = (lengthFieldOffset + lengthFieldLength) + lengthFieldValue + lengthAdjustment</p>
     */
    public static FrameDecoder lengthField(
            int lengthFieldOffset,
            int lengthFieldLength,
            LengthFieldFrameDecoder.Endian endian,
            int lengthAdjustment,
            int initialBytesToStrip,
            int maxFrameLength
    ) {
        return new LengthFieldFrameDecoder(
                lengthFieldOffset,
                lengthFieldLength,
                endian,
                lengthAdjustment,
                initialBytesToStrip,
                maxFrameLength
        );
    }

    public static LengthFieldBuilder lengthFieldBuilder() {
        return new LengthFieldBuilder();
    }

    public static final class LengthFieldBuilder {
        private int lengthFieldOffset;
        private int lengthFieldLength;
        private LengthFieldFrameDecoder.Endian endian = LengthFieldFrameDecoder.Endian.BIG;
        private int lengthAdjustment = 0;
        private int initialBytesToStrip = 0;
        private int maxFrameLength = 4096;

        public LengthFieldBuilder lengthFieldOffset(int offset) {
            this.lengthFieldOffset = offset;
            return this;
        }

        public LengthFieldBuilder lengthFieldLength(int len) {
            this.lengthFieldLength = len;
            return this;
        }

        public LengthFieldBuilder endian(LengthFieldFrameDecoder.Endian endian) {
            if (endian == null) throw new IllegalArgumentException("endian == null");
            this.endian = endian;
            return this;
        }

        /**
         * Adds to decoded length (use when length represents payload only, etc.)
         */
        public LengthFieldBuilder lengthAdjustment(int adjustment) {
            this.lengthAdjustment = adjustment;
            return this;
        }

        /**
         * Strip N bytes from the beginning of the emitted frame (0 keeps full frame).
         */
        public LengthFieldBuilder initialBytesToStrip(int strip) {
            this.initialBytesToStrip = strip;
            return this;
        }

        public LengthFieldBuilder maxFrameLength(int maxFrameLength) {
            this.maxFrameLength = maxFrameLength;
            return this;
        }

        public FrameDecoder build() {
            return new LengthFieldFrameDecoder(
                    lengthFieldOffset,
                    lengthFieldLength,
                    endian,
                    lengthAdjustment,
                    initialBytesToStrip,
                    maxFrameLength
            );
        }
    }
}



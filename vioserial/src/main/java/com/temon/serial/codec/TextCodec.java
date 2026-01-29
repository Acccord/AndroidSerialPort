package com.temon.serial.codec;

import java.nio.charset.Charset;

/**
 * Explicit charset conversions for text-based protocols.
 *
 * <p>This avoids hard-coded encodings (GBK/GB18030) in a protocol-agnostic library.</p>
 */
public final class TextCodec {
    private TextCodec() {
    }

    public static String toHex(String text, Charset charset) {
        if (text == null) return "";
        if (charset == null) throw new IllegalArgumentException("charset == null");
        byte[] bytes = text.getBytes(charset);
        return HexCodec.encode(bytes, 0, bytes.length);
    }

    public static String fromHex(String hex, Charset charset) {
        if (charset == null) throw new IllegalArgumentException("charset == null");
        byte[] bytes = HexCodec.decode(hex);
        return new String(bytes, charset);
    }
}



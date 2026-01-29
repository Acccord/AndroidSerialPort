package com.temon.serial.codec;

/**
 * Minimal hex codec for binary protocols.
 *
 * <p>Design goals: tiny API surface, no charset assumptions, no allocations beyond output.</p>
 */
public final class HexCodec {
    private HexCodec() {
    }

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    /** Encode to hex without spaces. */
    public static String encode(byte[] data, int offset, int length) {
        if (data == null) return "";
        if (length <= 0) return "";
        char[] out = new char[length * 2];
        int p = 0;
        for (int i = 0; i < length; i++) {
            int v = data[offset + i] & 0xFF;
            out[p++] = HEX[v >>> 4];
            out[p++] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    /** Encode to hex with a single space between bytes. */
    public static String encodeSpaced(byte[] data, int offset, int length) {
        if (data == null) return "";
        if (length <= 0) return "";
        char[] out = new char[length * 3 - 1];
        int p = 0;
        for (int i = 0; i < length; i++) {
            int v = data[offset + i] & 0xFF;
            out[p++] = HEX[v >>> 4];
            out[p++] = HEX[v & 0x0F];
            if (i != length - 1) out[p++] = ' ';
        }
        return new String(out);
    }

    /** Decode hex string (spaces allowed). Returns empty array on empty input. */
    public static byte[] decode(String hex) {
        if (hex == null) return new byte[0];
        String s = hex.replace(" ", "").trim();
        if (s.isEmpty()) return new byte[0];
        int n = s.length();
        if ((n & 1) == 1) {
            s = "0" + s;
            n++;
        }
        byte[] out = new byte[n / 2];
        for (int i = 0; i < n; i += 2) {
            int hi = fromHex(s.charAt(i));
            int lo = fromHex(s.charAt(i + 1));
            if (hi < 0 || lo < 0) throw new IllegalArgumentException("invalid hex: " + hex);
            out[i / 2] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    private static int fromHex(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return 10 + (c - 'a');
        if (c >= 'A' && c <= 'F') return 10 + (c - 'A');
        return -1;
    }
}



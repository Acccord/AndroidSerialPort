package com.temon.androidserialport

object SerialInputUtils {
    fun normalizeHex(input: String): String {
        return input.replace("\\s".toRegex(), "")
    }

    fun isValidHex(input: String): Boolean {
        val hex = normalizeHex(input)
        if (hex.isEmpty()) return false
        if (hex.length % 2 != 0) return false
        for (c in hex) {
            val ok = (c in '0'..'9') || (c in 'a'..'f') || (c in 'A'..'F')
            if (!ok) return false
        }
        return true
    }

    fun formatHex(hex: String): String {
        val normalized = normalizeHex(hex)
        return normalized.chunked(2).joinToString(" ")
    }
}

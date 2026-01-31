package com.temon.androidserialport

import android.content.Context

class SerialPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLogAutoScroll(): Boolean = prefs.getBoolean(KEY_LOG_AUTO_SCROLL, true)

    fun setLogAutoScroll(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOG_AUTO_SCROLL, enabled).apply()
    }

    fun getLogShowTime(): Boolean = prefs.getBoolean(KEY_LOG_SHOW_TIME, true)

    fun setLogShowTime(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOG_SHOW_TIME, enabled).apply()
    }

    fun getLogShowTitle(): Boolean = prefs.getBoolean(KEY_LOG_SHOW_TITLE, true)

    fun setLogShowTitle(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOG_SHOW_TITLE, enabled).apply()
    }

    fun getInputModeHex(): Boolean = prefs.getBoolean(KEY_INPUT_MODE_HEX, true)

    fun setInputModeHex(isHex: Boolean) {
        prefs.edit().putBoolean(KEY_INPUT_MODE_HEX, isHex).apply()
    }

    fun getLastSuccessfulBaud(): Int? {
        val value = prefs.getInt(KEY_LAST_SUCCESS_BAUD, -1)
        return if (value > 0) value else null
    }

    fun setLastSuccessfulBaud(baud: Int) {
        prefs.edit().putInt(KEY_LAST_SUCCESS_BAUD, baud).apply()
    }

    fun getLastSuccessfulPort(): String? {
        val value = prefs.getString(KEY_LAST_SUCCESS_PORT, null)
        return value?.takeIf { it.isNotBlank() }
    }

    fun setLastSuccessfulPort(port: String) {
        prefs.edit().putString(KEY_LAST_SUCCESS_PORT, port).apply()
    }

    fun getCommonCommandsJson(): String {
        return prefs.getString(KEY_COMMON_COMMANDS, DEFAULT_COMMON_COMMANDS_JSON)
            ?: DEFAULT_COMMON_COMMANDS_JSON
    }

    fun setCommonCommandsJson(raw: String) {
        prefs.edit().putString(KEY_COMMON_COMMANDS, raw).apply()
    }

    fun getCommonCommandsAdded(): Boolean = prefs.getBoolean(KEY_COMMON_COMMANDS_ADDED, false)

    fun setCommonCommandsAdded(added: Boolean) {
        prefs.edit().putBoolean(KEY_COMMON_COMMANDS_ADDED, added).apply()
    }

    companion object {
        private const val PREFS_NAME = "serial_prefs"
        private const val KEY_LOG_AUTO_SCROLL = "pref_log_auto_scroll"
        private const val KEY_LOG_SHOW_TIME = "pref_log_show_time"
        private const val KEY_LOG_SHOW_TITLE = "pref_log_show_title"
        private const val KEY_INPUT_MODE_HEX = "pref_input_mode_hex"
        private const val KEY_LAST_SUCCESS_BAUD = "last_success_baud"
        private const val KEY_LAST_SUCCESS_PORT = "last_success_port"
        private const val KEY_COMMON_COMMANDS = "common_commands"
        private const val KEY_COMMON_COMMANDS_ADDED = "common_commands_added"
        private const val DEFAULT_COMMON_COMMANDS_JSON = "[]"
    }
}

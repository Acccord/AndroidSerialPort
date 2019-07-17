package com.vi.vioserial.util;

import android.util.Log;

/**
 * @author Vi
 * @date 2019-07-17 17:40
 * @e-mail cfop_f2l@163.com
 */

public class Logger {
    private static Logger mInstace;
    private static final String COMMON_TAG = "serial";
    public static boolean SHOW_LOG = false;

    public static Logger getInstace() {
        if (mInstace == null)
            mInstace = new Logger();
        return mInstace;
    }

    public void v(String tag, String msg) {
        if (SHOW_LOG) {
            Log.v(COMMON_TAG, "[" + tag + "] " + msg);
        }
    }

    public void d(String tag, String msg) {
        if (SHOW_LOG) {
            Log.d(COMMON_TAG, "[" + tag + "] " + msg);
        }
    }

    public void i(String tag, String msg) {
        if (SHOW_LOG) {
            Log.i(COMMON_TAG, "[" + tag + "] " + msg);
        }
    }

    public void w(String tag, String msg) {
        Log.w(COMMON_TAG, "[" + tag + "] " + msg);
    }

    public void e(String tag, String msg) {
        Log.e(COMMON_TAG, "[" + tag + "] " + msg);
    }

    public void v(String tag, String msg, Throwable tr) {
        if (SHOW_LOG) {
            Log.v(COMMON_TAG, "[" + tag + "] " + msg, tr);
        }
    }

    public void d(String tag, String msg, Throwable tr) {
        if (SHOW_LOG) {
            Log.d(COMMON_TAG, "[" + tag + "] " + msg, tr);
        }
    }

    public void i(String tag, String msg, Throwable tr) {
        if (SHOW_LOG) {
            Log.i(COMMON_TAG, "[" + tag + "] " + msg, tr);
        }
    }

    public void w(String tag, String msg, Throwable tr) {
        Log.w(COMMON_TAG, "[" + tag + "] " + msg, tr);
    }

    public void e(String tag, String msg, Throwable tr) {
        Log.e(COMMON_TAG, "[" + tag + "] " + msg, tr);
    }
}

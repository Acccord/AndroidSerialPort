package com.temon.serial.internal.dispatch;

import android.os.Handler;
import android.os.Looper;
import com.temon.serial.core.CallbackDispatcher;

public final class MainThreadDispatcher implements CallbackDispatcher {
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void dispatch(Runnable r) {
        handler.post(r);
    }
}

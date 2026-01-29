package com.temon.serial.core;

import com.temon.serial.internal.dispatch.DirectDispatcher;
import com.temon.serial.internal.dispatch.MainThreadDispatcher;

/**
 * Factory for common callback dispatchers.
 */
public final class Dispatchers {
    private Dispatchers() {
    }

    /**
     * Dispatch callbacks inline on the caller thread.
     */
    public static CallbackDispatcher direct() {
        return new DirectDispatcher();
    }

    /**
     * Dispatch callbacks on Android main thread.
     */
    public static CallbackDispatcher mainThread() {
        return new MainThreadDispatcher();
    }
}

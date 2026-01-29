package com.temon.serial.internal.dispatch;

import com.temon.serial.core.CallbackDispatcher;

/**
 * Dispatches callbacks inline on the caller thread.
 */
public final class DirectDispatcher implements CallbackDispatcher {
    @Override
    public void dispatch(Runnable r) {
        r.run();
    }
}

package com.temon.serial.core;

/**
 * Dispatches callbacks to a specific thread/executor.
 *
 * <p>Use {@link Dispatchers} to obtain common implementations. Internal dispatcher
 * implementations live under {@code com.temon.serial.internal} and are not part of the public API.</p>
 */
public interface CallbackDispatcher {
    void dispatch(Runnable r);
}



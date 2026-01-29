package com.temon.serial.core;

public final class SerialException extends Exception {
    private final SerialError error;

    public SerialException(SerialError error, String message) {
        super(message);
        this.error = error;
    }

    public SerialException(SerialError error, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    public SerialError getError() {
        return error;
    }
}



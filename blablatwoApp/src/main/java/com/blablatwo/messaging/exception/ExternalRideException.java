package com.blablatwo.messaging.exception;

public class ExternalRideException extends RuntimeException {
    public ExternalRideException() {
        super("External rides not supported");
    }
}

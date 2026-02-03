package com.blablatwo.messaging.exception;

public class InvalidDriverException extends RuntimeException {
    public InvalidDriverException(Long providedDriverId, Long actualDriverId) {
        super("Driver ID does not match ride driver");
    }
}

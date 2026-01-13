package com.blablatwo.exceptions;

public class DuplicateExternalRideException extends RuntimeException {

    public DuplicateExternalRideException(String identifier) {
        super("External ride already exists: " + identifier);
    }
}

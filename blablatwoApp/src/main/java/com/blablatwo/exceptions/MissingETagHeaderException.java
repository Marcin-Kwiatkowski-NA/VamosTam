package com.blablatwo.exceptions;

public class MissingETagHeaderException extends RuntimeException {
    public MissingETagHeaderException() {
        super("Missing eTag header");
    }
}


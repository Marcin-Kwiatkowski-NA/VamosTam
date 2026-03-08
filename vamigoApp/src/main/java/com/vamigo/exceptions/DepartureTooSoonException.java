package com.vamigo.exceptions;

public class DepartureTooSoonException extends RuntimeException {
    public DepartureTooSoonException(String message) {
        super(message);
    }
}
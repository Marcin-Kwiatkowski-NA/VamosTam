package com.blablatwo.exceptions;

public class CannotCreateRideException extends RuntimeException {
    public CannotCreateRideException(Long userId) {
        super("User " + userId + " cannot create rides");
    }
}

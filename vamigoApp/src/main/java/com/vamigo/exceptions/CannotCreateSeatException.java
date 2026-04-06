package com.vamigo.exceptions;

public class CannotCreateSeatException extends RuntimeException {
    public CannotCreateSeatException(Long userId) {
        super("User " + userId + " cannot create seat offers");
    }
}

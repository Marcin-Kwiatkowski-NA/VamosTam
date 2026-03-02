package com.vamigo.exceptions;

public class CannotBookException extends RuntimeException {
    public CannotBookException(Long userId) {
        super("User " + userId + " cannot book rides");
    }
}

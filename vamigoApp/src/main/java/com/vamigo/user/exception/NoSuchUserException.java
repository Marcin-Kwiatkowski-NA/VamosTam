package com.vamigo.user.exception;

public class NoSuchUserException extends RuntimeException {

    public NoSuchUserException(Long userId) {
        super("No user found with id: " + userId);
    }

    public NoSuchUserException(String message) {
        super(message);
    }
}

package com.vamigo.auth.exception;

public class MissingProviderEmailException extends RuntimeException {

    private static final String DEFAULT_MESSAGE =
            "The identity provider did not supply an email address. Please grant email permission and try again.";

    public MissingProviderEmailException() {
        super(DEFAULT_MESSAGE);
    }

    public MissingProviderEmailException(String message) {
        super(message);
    }
}

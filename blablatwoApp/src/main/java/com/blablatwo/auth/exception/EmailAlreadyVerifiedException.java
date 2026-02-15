package com.blablatwo.auth.exception;

public class EmailAlreadyVerifiedException extends RuntimeException {

    public EmailAlreadyVerifiedException() {
        super("Email is already verified");
    }
}

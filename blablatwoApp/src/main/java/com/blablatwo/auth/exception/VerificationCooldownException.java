package com.blablatwo.auth.exception;

public class VerificationCooldownException extends RuntimeException {

    public VerificationCooldownException(int cooldownSeconds) {
        super("Please wait %d seconds before requesting another verification email".formatted(cooldownSeconds));
    }
}

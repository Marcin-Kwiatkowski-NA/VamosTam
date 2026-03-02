package com.vamigo.exceptions;

public class FacebookBotMissingException extends RuntimeException {
    public FacebookBotMissingException() {
        super("Facebook bot account not found. Ensure DataInitializer has run.");
    }
}

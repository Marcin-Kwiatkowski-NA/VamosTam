package com.vamigo.contact;

public class ContactRateLimitException extends RuntimeException {

    public ContactRateLimitException() {
        super("Too many contact form submissions. Please try again later.");
    }
}

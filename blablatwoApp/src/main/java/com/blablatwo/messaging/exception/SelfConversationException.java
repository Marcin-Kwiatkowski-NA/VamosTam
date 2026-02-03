package com.blablatwo.messaging.exception;

public class SelfConversationException extends RuntimeException {
    public SelfConversationException() {
        super("Cannot start a conversation with yourself");
    }
}

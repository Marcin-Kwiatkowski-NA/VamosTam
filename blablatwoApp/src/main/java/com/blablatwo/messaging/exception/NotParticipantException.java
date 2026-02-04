package com.blablatwo.messaging.exception;

import java.util.UUID;

public class NotParticipantException extends RuntimeException {
    public NotParticipantException(UUID conversationId, Long userId) {
        super("Not a participant in conversation: " + conversationId);
    }
}

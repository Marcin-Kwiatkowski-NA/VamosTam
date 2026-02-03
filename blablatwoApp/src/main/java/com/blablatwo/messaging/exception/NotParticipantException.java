package com.blablatwo.messaging.exception;

import java.util.UUID;

public class NotParticipantException extends RuntimeException {
    public NotParticipantException(UUID conversationId, Long travelerId) {
        super("Not a participant in conversation: " + conversationId);
    }
}

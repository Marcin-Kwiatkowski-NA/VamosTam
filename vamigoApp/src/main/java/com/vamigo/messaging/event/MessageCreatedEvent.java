package com.vamigo.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record MessageCreatedEvent(
    UUID messageId,
    UUID conversationId,
    Long senderId,
    Instant createdAt
) {}

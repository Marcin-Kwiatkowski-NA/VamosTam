package com.blablatwo.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageDto(
    UUID id,
    UUID conversationId,
    Long senderId,
    boolean isMine,
    String body,
    Instant createdAt
) {}

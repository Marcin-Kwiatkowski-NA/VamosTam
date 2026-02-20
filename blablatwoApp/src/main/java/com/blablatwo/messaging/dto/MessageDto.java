package com.blablatwo.messaging.dto;

import com.blablatwo.messaging.MessageStatus;

import java.time.Instant;
import java.util.UUID;

public record MessageDto(
    UUID id,
    UUID conversationId,
    Long senderId,
    boolean isMine,
    String body,
    Instant createdAt,
    MessageStatus status
) {}

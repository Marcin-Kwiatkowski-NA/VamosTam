package com.vamigo.messaging.dto;

import com.vamigo.messaging.MessageStatus;
import com.vamigo.messaging.MessageType;

import java.time.Instant;
import java.util.UUID;

public record MessageDto(
    UUID id,
    UUID conversationId,
    Long senderId,
    boolean isMine,
    String body,
    Instant createdAt,
    MessageStatus status,
    MessageType messageType
) {}

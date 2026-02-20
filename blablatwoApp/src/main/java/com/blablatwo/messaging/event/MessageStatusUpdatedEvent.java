package com.blablatwo.messaging.event;

import com.blablatwo.messaging.MessageStatus;

import java.util.UUID;

public record MessageStatusUpdatedEvent(
    UUID conversationId,
    MessageStatus newStatus,
    Long senderId
) {}

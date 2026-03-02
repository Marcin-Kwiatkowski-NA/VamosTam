package com.vamigo.messaging.event;

import com.vamigo.messaging.MessageStatus;

import java.util.UUID;

public record MessageStatusUpdatedEvent(
    UUID conversationId,
    MessageStatus newStatus,
    Long senderId
) {}

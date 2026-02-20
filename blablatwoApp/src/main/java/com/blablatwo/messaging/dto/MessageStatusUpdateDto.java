package com.blablatwo.messaging.dto;

import com.blablatwo.messaging.MessageStatus;

import java.util.UUID;

public record MessageStatusUpdateDto(UUID conversationId, MessageStatus status) {}

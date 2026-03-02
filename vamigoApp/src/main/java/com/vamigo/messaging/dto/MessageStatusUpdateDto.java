package com.vamigo.messaging.dto;

import com.vamigo.messaging.MessageStatus;

import java.util.UUID;

public record MessageStatusUpdateDto(UUID conversationId, MessageStatus status) {}

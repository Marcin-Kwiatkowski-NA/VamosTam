package com.vamigo.messaging.dto;

import java.util.UUID;

public record StompErrorDto(
    String code,
    String message,
    UUID conversationId
) {}

package com.blablatwo.messaging.dto;

import java.util.UUID;

public record MarkReadRequest(UUID lastMessageId) {}

package com.vamigo.messaging.dto;

import java.util.UUID;

public record MarkReadRequest(UUID lastMessageId) {}

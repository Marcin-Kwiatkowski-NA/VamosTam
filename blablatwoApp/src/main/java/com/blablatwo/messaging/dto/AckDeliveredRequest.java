package com.blablatwo.messaging.dto;

import java.util.UUID;

public record AckDeliveredRequest(UUID lastMessageId) {}

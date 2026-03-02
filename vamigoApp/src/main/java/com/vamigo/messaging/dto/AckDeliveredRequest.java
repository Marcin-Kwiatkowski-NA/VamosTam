package com.vamigo.messaging.dto;

import java.util.UUID;

public record AckDeliveredRequest(UUID lastMessageId) {}

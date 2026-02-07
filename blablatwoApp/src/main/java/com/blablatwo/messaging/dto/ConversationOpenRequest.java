package com.blablatwo.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConversationOpenRequest(
    @NotBlank String topicKey,
    @NotNull Long peerUserId
) {}

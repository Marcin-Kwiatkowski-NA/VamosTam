package com.blablatwo.messaging.dto;

import jakarta.validation.constraints.NotNull;

public record CreateConversationRequest(
    @NotNull Long rideId,
    @NotNull Long driverId
) {}

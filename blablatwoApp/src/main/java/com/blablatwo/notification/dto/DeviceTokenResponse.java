package com.blablatwo.notification.dto;

import com.blablatwo.notification.Platform;

import java.time.Instant;

public record DeviceTokenResponse(
        Long id,
        Platform platform,
        Instant createdAt
) {
}

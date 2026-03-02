package com.vamigo.notification.dto;

import com.vamigo.notification.Platform;

import java.time.Instant;

public record DeviceTokenResponse(
        Long id,
        Platform platform,
        Instant createdAt
) {
}

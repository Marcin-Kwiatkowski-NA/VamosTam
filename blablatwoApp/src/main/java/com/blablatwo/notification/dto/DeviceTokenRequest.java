package com.blablatwo.notification.dto;

import com.blablatwo.notification.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceTokenRequest(
        @NotBlank(message = "Token cannot be blank")
        String token,

        @NotNull(message = "Platform is required")
        Platform platform
) {
}

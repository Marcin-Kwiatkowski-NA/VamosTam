package com.blablatwo.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleTokenRequest(
        @NotBlank(message = "ID token is required")
        String idToken
) {
}

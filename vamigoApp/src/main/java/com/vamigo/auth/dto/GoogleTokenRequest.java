package com.vamigo.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleTokenRequest(
        @NotBlank(message = "ID token is required")
        String idToken
) {
}

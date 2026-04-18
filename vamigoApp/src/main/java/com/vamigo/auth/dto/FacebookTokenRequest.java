package com.vamigo.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record FacebookTokenRequest(
        @NotBlank(message = "ID token is required")
        String idToken
) {
}

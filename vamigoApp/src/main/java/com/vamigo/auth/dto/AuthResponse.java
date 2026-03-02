package com.vamigo.auth.dto;

import com.vamigo.user.dto.UserProfileDto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        Long refreshExpiresIn,
        UserProfileDto user
) {
    public AuthResponse(String accessToken, String refreshToken,
                        Long expiresIn, Long refreshExpiresIn,
                        UserProfileDto user) {
        this(accessToken, refreshToken, "Bearer", expiresIn, refreshExpiresIn, user);
    }
}

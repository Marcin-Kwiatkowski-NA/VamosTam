package com.blablatwo.auth.dto;

import com.blablatwo.traveler.TravelerResponseDto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        Long refreshExpiresIn,
        TravelerResponseDto user
) {
    public AuthResponse(String accessToken, String refreshToken,
                        Long expiresIn, Long refreshExpiresIn,
                        TravelerResponseDto user) {
        this(accessToken, refreshToken, "Bearer", expiresIn, refreshExpiresIn, user);
    }
}

package com.blablatwo.auth.dto;

import com.blablatwo.traveler.TravelerResponseDto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Long expiresIn,
        TravelerResponseDto user
) {
    public AuthResponse(String accessToken, Long expiresIn, TravelerResponseDto user) {
        this(accessToken, "Bearer", expiresIn, user);
    }
}

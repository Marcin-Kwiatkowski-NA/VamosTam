package com.blablatwo.traveler;

import lombok.Builder;

@Builder(toBuilder = true)
public record DriverProfileDto(
        Long id,
        String username,
        String name,
        String email,
        String phoneNumber
) {
}
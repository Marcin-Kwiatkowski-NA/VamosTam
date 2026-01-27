package com.blablatwo.traveler;

import lombok.Builder;

@Builder(toBuilder = true)
public record TravelerProfileDto(
        Long id,
        String username,
        String name,
        String email,
        String phoneNumber
) {
}
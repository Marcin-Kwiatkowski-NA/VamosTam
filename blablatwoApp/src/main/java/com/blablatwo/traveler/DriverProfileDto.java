package com.blablatwo.traveler;

public record DriverProfileDto(
        Long id,
        String username,
        String name,
        String email,
        String phoneNumber
) {}
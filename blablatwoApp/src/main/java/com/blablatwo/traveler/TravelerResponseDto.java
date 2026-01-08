package com.blablatwo.traveler;

import com.blablatwo.vehicle.VehicleResponseDto;

import java.util.List;

public record TravelerResponseDto(
        Long id,
        String username,
        String email,
        String phoneNumber,
        String name,
        Role role,
        String pictureUrl,
        List<VehicleResponseDto> vehicles
) {
}
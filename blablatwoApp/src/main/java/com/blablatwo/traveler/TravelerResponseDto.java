package com.blablatwo.traveler;

import com.blablatwo.config.Roles;
import com.blablatwo.vehicle.VehicleResponseDto;

import java.util.List;

public record TravelerResponseDto(
        Long id,
        String username,
        String email,
        String phoneNumber,
        String name,
        Roles authority,
        TravelerType type,
        String pictureUrl,
        List<VehicleResponseDto> vehicles
) {
}
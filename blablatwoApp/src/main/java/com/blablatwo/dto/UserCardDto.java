package com.blablatwo.dto;

import com.blablatwo.vehicle.VehicleResponseDto;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
public record UserCardDto(
        Long id,
        String name,
        Double rating,
        Integer completedRides,
        String avatarUrl,
        String bio,
        boolean emailVerified,
        boolean phoneVerified,
        int ridesGiven,
        int ridesTaken,
        int ratingCount,
        List<VehicleResponseDto> vehicles
) {
}

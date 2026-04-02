package com.vamigo.vehicle;

import lombok.Builder;

@Builder(toBuilder = true)
public record VehicleResponseDto(
        Long id,
        String make,
        String model,
        Integer productionYear,
        VehicleColor color,
        String licensePlate,
        String photoUrl
) {
}
package com.blablatwo.vehicle;

import lombok.Builder;

@Builder(toBuilder = true)
public record VehicleResponseDto(
        Long id,
        String make,
        String model,
        Integer productionYear,
        String color,
        String licensePlate
) {
}
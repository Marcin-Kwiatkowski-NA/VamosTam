package com.blablatwo.vehicle;

public record VehicleResponseDto(
        Long id,
        String make,
        String model,
        Integer productionYear,
        String color,
        String licensePlate
) {
}
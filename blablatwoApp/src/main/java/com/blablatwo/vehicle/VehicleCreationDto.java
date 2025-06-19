package com.blablatwo.vehicle;

public record VehicleCreationDto(
        String make,
        String model,
        Integer productionYear,
        String color,
        String licensePlate
) {
}
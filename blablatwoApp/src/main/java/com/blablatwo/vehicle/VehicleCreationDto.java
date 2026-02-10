package com.blablatwo.vehicle;

import lombok.Builder;

@Builder(toBuilder = true)
public record VehicleCreationDto(
        String make,
        String model,
        Integer productionYear,
        String color,
        String licensePlate
) {
}
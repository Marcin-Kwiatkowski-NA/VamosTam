package com.vamigo.vehicle;

import lombok.Builder;

@Builder(toBuilder = true)
public record VehicleCreationDto(
        String make,
        String model,
        Integer productionYear,
        VehicleColor color,
        String licensePlate
) {
}
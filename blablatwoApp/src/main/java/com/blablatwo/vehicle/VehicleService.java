package com.blablatwo.vehicle;

import java.util.List;
import java.util.Optional;

public interface VehicleService {
    Optional<Vehicle> getById(Long id);

    List<Vehicle> getAllVehicles();

    VehicleResponseDto create(VehicleCreationDto vehicleDto);

    VehicleResponseDto update(VehicleCreationDto vehicleDto, Long id);

    void delete(Long id);
}
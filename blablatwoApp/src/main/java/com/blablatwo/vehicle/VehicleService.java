package com.blablatwo.vehicle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException; // Added import
import java.util.Optional;
import java.util.stream.Collectors;

public interface VehicleService {
    Optional<Vehicle> getById(Long id);
    List<Vehicle> getAllVehicles();
    VehicleResponseDto create(VehicleCreationDto vehicleDto);
    VehicleResponseDto update(VehicleCreationDto vehicleDto, Long id);
    void delete(Long id);
}
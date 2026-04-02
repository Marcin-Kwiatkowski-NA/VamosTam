package com.vamigo.vehicle;

import java.util.List;

public interface VehicleService {

    List<VehicleResponseDto> getMyVehicles(Long userId);

    VehicleResponseDto create(Long userId, VehicleCreationDto dto);

    VehicleResponseDto update(Long userId, Long vehicleId, VehicleCreationDto dto);

    void delete(Long userId, Long vehicleId);
}

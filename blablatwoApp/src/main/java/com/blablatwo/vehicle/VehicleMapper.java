package com.blablatwo.vehicle;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    VehicleResponseDto vehicleEntityToVehicleResponseDto(Vehicle vehicle);

    @Mapping(target = "id", ignore = true)
    Vehicle vehicleCreationDtoToEntity(VehicleCreationDto vehicleDto);

    @Mapping(target = "id", ignore = true)
    void update(@MappingTarget Vehicle vehicle, VehicleCreationDto vehicleDto);
}
package com.vamigo.vehicle;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    @Mapping(target = "photoUrl", ignore = true)
    VehicleResponseDto vehicleEntityToVehicleResponseDto(Vehicle vehicle);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "photoObjectKey", ignore = true)
    Vehicle vehicleCreationDtoToEntity(VehicleCreationDto vehicleDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "photoObjectKey", ignore = true)
    void update(@MappingTarget Vehicle vehicle, VehicleCreationDto vehicleDto);
}
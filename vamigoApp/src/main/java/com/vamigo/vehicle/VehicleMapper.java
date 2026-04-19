package com.vamigo.vehicle;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    @Mapping(target = "photoUrl", ignore = true)
    VehicleResponseDto vehicleEntityToVehicleResponseDto(Vehicle vehicle);

    VehicleDetails vehicleCreationDtoToDetails(VehicleCreationDto vehicleDto);
}

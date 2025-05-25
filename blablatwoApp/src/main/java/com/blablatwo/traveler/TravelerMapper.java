package com.blablatwo.traveler;

import com.blablatwo.vehicle.Vehicle;
import com.blablatwo.vehicle.VehicleResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TravelerMapper {

    TravelerResponseDto travelerEntityToTravelerResponseDto(Traveler traveler);

    List<VehicleResponseDto> mapVehicles(List<Vehicle> vehicles);
    VehicleResponseDto map(Vehicle vehicle);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", constant = "1")
    @Mapping(target = "authority", defaultValue = "ROLE_PASSENGER")
    @Mapping(target = "type", defaultValue = "PASSENGER")
    @Mapping(target = "ridesAsDriver", ignore = true)
    @Mapping(target = "ridesAsPassenger", ignore = true)
    @Mapping(target = "vehicles", ignore = true)
    Traveler travelerCreationDtoToEntity(TravelerCreationDto travelerDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "authority", ignore = true)
    @Mapping(target = "ridesAsDriver", ignore = true)
    @Mapping(target = "ridesAsPassenger", ignore = true)
    @Mapping(target = "vehicles", ignore = true)
    void update(@MappingTarget Traveler traveler, TravelerCreationDto travelerDto);
}
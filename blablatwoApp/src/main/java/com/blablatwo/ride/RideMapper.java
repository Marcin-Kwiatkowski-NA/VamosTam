package com.blablatwo.ride;

import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.traveler.DriverProfileDto;
import com.blablatwo.traveler.Traveler;
import com.blablatwo.vehicle.Vehicle;
import com.blablatwo.vehicle.VehicleResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RideMapper {

//    @Mapping(target = "lastModified", defaultExpression ="java(Instant.now())")
    RideResponseDto rideEntityToRideResponseDto(Ride ride);

    Ride rideCreationDtoToEntity(RideCreationDto ride);

    VehicleResponseDTO map(Vehicle value);
    DriverProfileDto map(Traveler value);
    void update(@MappingTarget Ride ride, RideCreationDto rideDTO);
}

package com.blablatwo.ride;

import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.traveler.DriverProfileDto;
import com.blablatwo.traveler.Traveler;
import com.blablatwo.traveler.VehicleEntity;
import com.blablatwo.traveler.VehicleResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RideMapper {

//    @Mapping(target = "lastModified", defaultExpression ="java(Instant.now())")
    RideResponseDto rideEntityToRideResponseDto(Ride ride);

    Ride rideCreationDtoToEntity(RideCreationDto ride);

    VehicleResponseDTO map(VehicleEntity value);
    DriverProfileDto map(Traveler value);
    void update(@MappingTarget Ride ride, RideCreationDto rideDTO);
}

package com.blablatwo.ride;

import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.traveler.DriverProfileDto;
import com.blablatwo.traveler.TravelerEntity;
import com.blablatwo.traveler.VehicleEntity;
import com.blablatwo.traveler.VehicleResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RideMapper {

    @Mapping(target = "lastModified", defaultExpression ="java(Instant.now())")
    RideResponseDto rideEntityToRideResponseDto(RideEntity rideEntity);

    RideEntity rideCreationDtoToEntity(RideCreationDto ride);

    VehicleResponseDTO map(VehicleEntity value);
    DriverProfileDto map(TravelerEntity value);
    void update(@MappingTarget RideEntity rideEntity, RideCreationDto rideDTO);
}

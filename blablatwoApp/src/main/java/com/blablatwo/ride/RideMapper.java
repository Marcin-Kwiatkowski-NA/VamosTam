package com.blablatwo.ride;

import com.blablatwo.city.CityMapper;
import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.traveler.TravelerMapper;
import com.blablatwo.vehicle.VehicleMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring",
        uses = {CityMapper.class, TravelerMapper.class, VehicleMapper.class})
public abstract class RideMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rideStatus", constant = "OPEN")
    @Mapping(target = "lastModified", expression = "java(java.time.Instant.now())")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "passengers", ignore = true)
    public abstract Ride rideCreationDtoToEntity(RideCreationDto rideCreationDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "lastModified", expression = "java(java.time.Instant.now())")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "passengers", ignore = true)
    public abstract void update(@MappingTarget Ride ride, RideCreationDto rideDTO);

    public abstract RideResponseDto rideEntityToRideResponseDto(Ride ride);
}

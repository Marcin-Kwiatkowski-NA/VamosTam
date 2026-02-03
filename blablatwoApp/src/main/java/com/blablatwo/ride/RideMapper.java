package com.blablatwo.ride;

import com.blablatwo.city.CityMapper;
import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.vehicle.VehicleMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring",
        uses = {CityMapper.class, VehicleMapper.class})
public abstract class RideMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "rideStatus", constant = "OPEN")
    @Mapping(target = "lastModified", expression = "java(java.time.Instant.now())")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "passengers", ignore = true)
    @Mapping(target = "source", constant = "INTERNAL")
    @Mapping(target = "origin", ignore = true)
    @Mapping(target = "destination", ignore = true)
    public abstract Ride rideCreationDtoToEntity(RideCreationDto rideCreationDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "lastModified", expression = "java(java.time.Instant.now())")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "passengers", ignore = true)
    @Mapping(target = "source", ignore = true)
    @Mapping(target = "origin", ignore = true)
    @Mapping(target = "destination", ignore = true)
    @Mapping(target = "rideStatus", ignore = true)
    public abstract void update(@MappingTarget Ride ride, RideCreationDto rideDTO);

    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "contactMethods", ignore = true)
    @Mapping(target = "seatsTaken", expression = "java(ride.getPassengers() != null ? ride.getPassengers().size() : 0)")
    public abstract RideResponseDto rideEntityToRideResponseDto(Ride ride);
}

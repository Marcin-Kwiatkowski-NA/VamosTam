package com.blablatwo.ride;

import com.blablatwo.city.CityMapper;
import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.vehicle.VehicleMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring",
        uses = {CityMapper.class, VehicleMapper.class})
public abstract class RideMapper {

    @Autowired
    protected CityMapper cityMapper;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "lastModified", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "passengers", ignore = true)
    @Mapping(target = "source", constant = "INTERNAL")
    @Mapping(target = "segment", ignore = true)
    @Mapping(target = "timeSlot", ignore = true)
    public abstract Ride rideCreationDtoToEntity(RideCreationDto rideCreationDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "lastModified", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "passengers", ignore = true)
    @Mapping(target = "source", ignore = true)
    @Mapping(target = "segment", ignore = true)
    @Mapping(target = "timeSlot", ignore = true)
    @Mapping(target = "status", ignore = true)
    public abstract void update(@MappingTarget Ride ride, RideCreationDto rideDTO);

    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "contactMethods", ignore = true)
    @Mapping(target = "seatsTaken", expression = "java(ride.getPassengers() != null ? ride.getPassengers().size() : 0)")
    @Mapping(target = "origin", expression = "java(cityMapper.cityEntityToCityDto(ride.getSegment().getOrigin()))")
    @Mapping(target = "destination", expression = "java(cityMapper.cityEntityToCityDto(ride.getSegment().getDestination()))")
    @Mapping(target = "departureTime", expression = "java(ride.getTimeSlot().toLocalDateTime())")
    @Mapping(target = "isApproximate", expression = "java(ride.getTimeSlot().isApproximate())")
    @Mapping(target = "rideStatus", expression = "java(ride.computeRideStatus())")
    public abstract RideResponseDto rideEntityToRideResponseDto(Ride ride);
}

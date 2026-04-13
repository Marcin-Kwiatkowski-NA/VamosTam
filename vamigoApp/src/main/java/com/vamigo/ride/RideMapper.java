package com.vamigo.ride;

import com.vamigo.location.LocationMapper;
import com.vamigo.ride.dto.RideCreationDto;
import com.vamigo.ride.dto.RideListDto;
import com.vamigo.ride.dto.RideResponseDto;
import com.vamigo.vehicle.VehicleMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring",
        uses = {LocationMapper.class, VehicleMapper.class, RideStopMapper.class})
public abstract class RideMapper {

    @Autowired
    protected LocationMapper locationMapper;

    @Autowired
    protected RideStopMapper rideStopMapper;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "lastModified", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "source", constant = "INTERNAL")
    @Mapping(target = "stops", ignore = true)
    @Mapping(target = "bookings", ignore = true)
    @Mapping(target = "departureTime", ignore = true)
    @Mapping(target = "totalSeats", source = "availableSeats")
    @Mapping(target = "timePrecision", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "estimatedArrivalAt", ignore = true)
    public abstract Ride rideCreationDtoToEntity(RideCreationDto rideCreationDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "lastModified", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "source", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "stops", ignore = true)
    @Mapping(target = "bookings", ignore = true)
    @Mapping(target = "departureTime", ignore = true)
    @Mapping(target = "totalSeats", source = "availableSeats")
    @Mapping(target = "timePrecision", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "estimatedArrivalAt", ignore = true)
    public abstract void update(@MappingTarget Ride ride, RideCreationDto rideDTO);

    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "contactMethods", ignore = true)
    @Mapping(target = "bookings", ignore = true)
    @Mapping(target = "origin", expression = "java(locationMapper.locationToDto(ride.getOrigin()))")
    @Mapping(target = "destination", expression = "java(locationMapper.locationToDto(ride.getDestination()))")
    @Mapping(target = "stops", expression = "java(rideStopMapper.rideStopsToDtos(ride.getStops()))")
    @Mapping(target = "departureTime", source = "departureTime")
    @Mapping(target = "timePrecision", source = "timePrecision")
    @Mapping(target = "availableSeats", expression = "java(ride.getMinAvailableSeats())")
    @Mapping(target = "seatsTaken", expression = "java(ride.getTotalSeats() - ride.getMinAvailableSeats())")
    @Mapping(target = "rideStatus", expression = "java(ride.getRideStatus())")
    @Mapping(target = "bookingEnabled", constant = "true")
    public abstract RideResponseDto rideEntityToRideResponseDto(Ride ride);

    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "origin", expression = "java(locationMapper.locationToDto(ride.getOrigin()))")
    @Mapping(target = "destination", expression = "java(locationMapper.locationToDto(ride.getDestination()))")
    @Mapping(target = "stops", expression = "java(rideStopMapper.rideStopsToDtos(ride.getStops()))")
    @Mapping(target = "availableSeats", expression = "java(ride.getMinAvailableSeats())")
    @Mapping(target = "seatsTaken", expression = "java(ride.getTotalSeats() - ride.getMinAvailableSeats())")
    @Mapping(target = "rideStatus", expression = "java(ride.getRideStatus())")
    @Mapping(target = "bookingEnabled", constant = "true")
    public abstract RideListDto rideEntityToRideListDto(Ride ride);
}

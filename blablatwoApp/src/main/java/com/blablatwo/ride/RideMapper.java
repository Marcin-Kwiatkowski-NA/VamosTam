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
        uses = {CityMapper.class, VehicleMapper.class, RideStopMapper.class})
public abstract class RideMapper {

    @Autowired
    protected CityMapper cityMapper;

    @Autowired
    protected RideStopMapper rideStopMapper;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "lastModified", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "source", constant = "INTERNAL")
    @Mapping(target = "stops", ignore = true)
    @Mapping(target = "bookings", ignore = true)
    @Mapping(target = "departureDate", ignore = true)
    @Mapping(target = "departureTime", ignore = true)
    @Mapping(target = "totalSeats", source = "availableSeats")
    public abstract Ride rideCreationDtoToEntity(RideCreationDto rideCreationDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "lastModified", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "source", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "stops", ignore = true)
    @Mapping(target = "bookings", ignore = true)
    @Mapping(target = "departureDate", ignore = true)
    @Mapping(target = "departureTime", ignore = true)
    @Mapping(target = "totalSeats", source = "availableSeats")
    public abstract void update(@MappingTarget Ride ride, RideCreationDto rideDTO);

    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "contactMethods", ignore = true)
    @Mapping(target = "origin", expression = "java(cityMapper.cityEntityToCityDto(ride.getOrigin()))")
    @Mapping(target = "destination", expression = "java(cityMapper.cityEntityToCityDto(ride.getDestination()))")
    @Mapping(target = "stops", expression = "java(rideStopMapper.rideStopsToDtos(ride.getStops()))")
    @Mapping(target = "departureTime", expression = "java(ride.getDepartureDateTime())")
    @Mapping(target = "isApproximate", source = "approximate")
    @Mapping(target = "availableSeats", expression = "java(ride.getMinAvailableSeats())")
    @Mapping(target = "seatsTaken", expression = "java(ride.getTotalSeats() - ride.getMinAvailableSeats())")
    @Mapping(target = "rideStatus", expression = "java(ride.computeRideStatus())")
    public abstract RideResponseDto rideEntityToRideResponseDto(Ride ride);
}

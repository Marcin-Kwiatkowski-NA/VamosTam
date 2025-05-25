package com.blablatwo.ride;

import com.blablatwo.city.City;
import com.blablatwo.city.CityService;
import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.traveler.DriverProfileDto;
import com.blablatwo.traveler.Traveler;
import com.blablatwo.traveler.TravelerService;
import com.blablatwo.traveler.TravelerServiceImpl;
import com.blablatwo.vehicle.Vehicle;
import com.blablatwo.vehicle.VehicleResponseDto;
import com.blablatwo.vehicle.VehicleService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.NoSuchElementException;

@Mapper(componentModel = "spring")
public abstract class RideMapper {

    @Autowired
    protected CityService cityService;
    @Autowired
    protected TravelerService travelerService;
    @Autowired
    protected VehicleService vehicleService;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driver", source = "driverId", qualifiedByName = "mapDriver")
    @Mapping(target = "origin", source = "origin", qualifiedByName = "mapCity")
    @Mapping(target = "destination", source = "destination", qualifiedByName = "mapCity")
    @Mapping(target = "departureTime", source = "departureTime")
    @Mapping(target = "availableSeats", source = "availableSeats")
    @Mapping(target = "pricePerSeat", source = "pricePerSeat")
    @Mapping(target = "vehicle", source = "vehicleId", qualifiedByName = "mapVehicle")
    @Mapping(target = "rideStatus", constant = "OPEN")
    @Mapping(target = "lastModified", expression = "java(java.time.Instant.now())")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "passengers", ignore = true)
    public abstract Ride rideCreationDtoToEntity(RideCreationDto rideCreationDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "origin", source = "rideDTO.origin", qualifiedByName = "mapCity")
    @Mapping(target = "destination", source = "rideDTO.destination", qualifiedByName = "mapCity")
    @Mapping(target = "departureTime", source = "rideDTO.departureTime")
    @Mapping(target = "availableSeats", source = "rideDTO.availableSeats")
    @Mapping(target = "pricePerSeat", source = "rideDTO.pricePerSeat")
    @Mapping(target = "vehicle", source = "rideDTO.vehicleId", qualifiedByName = "mapVehicle")
    @Mapping(target = "rideStatus", ignore = true)
    @Mapping(target = "lastModified", expression = "java(java.time.Instant.now())")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "passengers", ignore = true)
    public abstract void update(@MappingTarget Ride ride, RideCreationDto rideDTO);

    @Named("mapCity")
    protected City mapCity(String cityName) {
        return cityService.findByName(cityName)
                .orElseThrow(() -> new IllegalArgumentException("City not found with name: " + cityName));
    }

    @Named("mapDriver")
    protected Traveler mapDriver(Long driverId) {
        if (driverId == null) {
            throw new IllegalArgumentException("Driver ID must be provided for a ride.");
        }
        return travelerService.getById(driverId)
                .orElseThrow(() -> new UsernameNotFoundException("Driver not found with ID: " + driverId));
    }

    @Named("mapVehicle")
    protected Vehicle mapVehicle(Long vehicleId) {
        if (vehicleId == null) {
            return null;
        }
        return vehicleService.getById(vehicleId)
                .orElseThrow(() -> new NoSuchElementException("Vehicle not found with ID: " + vehicleId));
    }

    public abstract RideResponseDto rideEntityToRideResponseDto(Ride ride);
    public abstract VehicleResponseDto map(Vehicle value);
    public abstract DriverProfileDto map(Traveler value);
}

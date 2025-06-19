package com.blablatwo.ride.dto;

import com.blablatwo.city.CityDto;
import com.blablatwo.ride.RideStatus;
import com.blablatwo.traveler.DriverProfileDto;
import com.blablatwo.vehicle.VehicleResponseDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record RideResponseDto(
        Long id,
        DriverProfileDto driver,
        CityDto origin,
        CityDto destination,
        LocalDateTime departureTime,
        Integer availableSeats,
        BigDecimal pricePerSeat,
        VehicleResponseDto vehicle,
        RideStatus rideStatus,
        Instant lastModified,
        List<DriverProfileDto> passengers
) {}
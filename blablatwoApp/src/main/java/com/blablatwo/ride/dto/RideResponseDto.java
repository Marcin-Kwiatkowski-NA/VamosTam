package com.blablatwo.ride.dto;

import com.blablatwo.city.CityDto;
import com.blablatwo.ride.RideSource;
import com.blablatwo.ride.RideStatus;
import com.blablatwo.traveler.DriverProfileDto;
import com.blablatwo.vehicle.VehicleResponseDto;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
public record RideResponseDto(
        Long id,
        DriverProfileDto driver,
        CityDto origin,
        CityDto destination,
        LocalDateTime departureTime,
        boolean isApproximate,
        RideSource source,
        Integer availableSeats,
        BigDecimal pricePerSeat,
        VehicleResponseDto vehicle,
        RideStatus rideStatus,
        Instant lastModified,
        List<DriverProfileDto> passengers,
        String sourceUrl
) {
}
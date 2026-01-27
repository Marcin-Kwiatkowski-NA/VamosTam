package com.blablatwo.ride.dto;

import com.blablatwo.city.CityDto;
import com.blablatwo.ride.RideSource;
import com.blablatwo.ride.RideStatus;
import com.blablatwo.vehicle.VehicleResponseDto;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
public record RideResponseDto(
        Long id,
        RideSource source,
        CityDto origin,
        CityDto destination,
        LocalDateTime departureTime,
        boolean isApproximate,
        BigDecimal pricePerSeat,
        int availableSeats,
        int seatsTaken,
        String description,
        DriverDto driver,
        List<ContactMethodDto> contactMethods,
        VehicleResponseDto vehicle,
        RideStatus rideStatus
) {
}

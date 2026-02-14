package com.blablatwo.ride.dto;

import com.blablatwo.dto.ContactMethodDto;
import com.blablatwo.dto.UserCardDto;
import com.blablatwo.location.LocationDto;
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
        LocationDto origin,
        LocationDto destination,
        List<RideStopDto> stops,
        LocalDateTime departureTime,
        boolean isApproximate,
        BigDecimal pricePerSeat,
        int availableSeats,
        int seatsTaken,
        int totalSeats,
        String description,
        UserCardDto driver,
        List<ContactMethodDto> contactMethods,
        VehicleResponseDto vehicle,
        RideStatus rideStatus
) {
}

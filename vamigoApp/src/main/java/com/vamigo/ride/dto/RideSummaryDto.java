package com.vamigo.ride.dto;

import com.vamigo.domain.Currency;
import com.vamigo.dto.UserCardDto;
import com.vamigo.location.LocationDto;
import com.vamigo.ride.RideStatus;
import com.vamigo.vehicle.VehicleResponseDto;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder(toBuilder = true)
public record RideSummaryDto(
        Long id,
        LocationDto origin,
        LocationDto destination,
        Instant departureTime,
        BigDecimal pricePerSeat,
        int totalSeats,
        UserCardDto driver,
        RideStatus rideStatus,
        Currency currency,
        VehicleResponseDto vehicle
) {
}

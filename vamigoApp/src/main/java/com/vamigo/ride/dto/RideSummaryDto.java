package com.vamigo.ride.dto;

import com.vamigo.dto.UserCardDto;
import com.vamigo.location.LocationDto;
import com.vamigo.ride.RideStatus;
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
        RideStatus rideStatus
) {
}

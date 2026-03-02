package com.vamigo.ride.dto;

import com.vamigo.dto.UserCardDto;
import com.vamigo.ride.BookingStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder(toBuilder = true)
public record BookingResponseDto(
        Long id,
        Long rideId,
        BookingStatus status,
        int seatCount,
        RideStopDto boardStop,
        RideStopDto alightStop,
        UserCardDto passenger,
        RideSummaryDto ride,
        Instant bookedAt,
        Instant resolvedAt,
        BigDecimal proposedPrice,
        String cancellationReason
) {
}

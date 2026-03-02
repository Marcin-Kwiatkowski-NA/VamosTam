package com.vamigo.ride.dto;

import com.vamigo.ride.BookingStatus;
import lombok.Builder;

import java.time.Instant;

@Builder
public record BookingNotificationDto(
        Long bookingId,
        Long rideId,
        BookingStatus status,
        String eventType,
        int seatCount,
        String rideOrigin,
        String rideDestination,
        Instant departureTime,
        String counterpartyName,
        String cancellationReason
) {
}

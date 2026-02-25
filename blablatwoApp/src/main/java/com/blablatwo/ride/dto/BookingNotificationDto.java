package com.blablatwo.ride.dto;

import com.blablatwo.ride.BookingStatus;
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

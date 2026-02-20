package com.blablatwo.ride.event;

public record BookingExpiredEvent(
        Long bookingId,
        Long rideId,
        Long passengerId,
        Long driverId
) {
}

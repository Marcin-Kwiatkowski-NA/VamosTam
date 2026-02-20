package com.blablatwo.ride.event;

public record BookingConfirmedEvent(
        Long bookingId,
        Long rideId,
        Long passengerId,
        Long driverId
) {
}

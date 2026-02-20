package com.blablatwo.ride.event;

public record BookingCancelledEvent(
        Long bookingId,
        Long rideId,
        Long passengerId,
        Long driverId,
        Long cancelledByUserId
) {
}

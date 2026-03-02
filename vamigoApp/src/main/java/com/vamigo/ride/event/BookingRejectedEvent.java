package com.vamigo.ride.event;

public record BookingRejectedEvent(
        Long bookingId,
        Long rideId,
        Long passengerId,
        Long driverId
) {
}

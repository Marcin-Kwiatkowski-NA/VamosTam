package com.vamigo.ride.event;

public record BookingRequestedEvent(
        Long bookingId,
        Long rideId,
        Long passengerId,
        Long driverId
) {
}

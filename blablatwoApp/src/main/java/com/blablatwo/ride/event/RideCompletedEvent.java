package com.blablatwo.ride.event;

import java.util.List;

public record RideCompletedEvent(
        Long rideId,
        Long driverId,
        List<Long> confirmedBookingIds
) {
}

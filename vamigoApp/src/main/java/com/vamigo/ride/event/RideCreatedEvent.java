package com.vamigo.ride.event;

import com.vamigo.ride.dto.RideResponseDto;

public record RideCreatedEvent(
        Long rideId,
        Long driverId,
        RideResponseDto ride
) {
}

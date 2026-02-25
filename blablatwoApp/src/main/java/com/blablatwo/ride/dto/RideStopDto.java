package com.blablatwo.ride.dto;

import com.blablatwo.location.LocationDto;

import java.time.Instant;

public record RideStopDto(
        int stopOrder,
        LocationDto location,
        Instant departureTime
) {
}

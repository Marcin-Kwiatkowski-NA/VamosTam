package com.vamigo.ride.dto;

import com.vamigo.location.LocationDto;

import java.time.Instant;

public record RideStopDto(
        int stopOrder,
        LocationDto location,
        Instant departureTime
) {
}

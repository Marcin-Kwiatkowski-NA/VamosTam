package com.blablatwo.ride.dto;

import com.blablatwo.location.LocationDto;

import java.time.LocalDateTime;

public record RideStopDto(
        int stopOrder,
        LocationDto location,
        LocalDateTime departureTime
) {
}

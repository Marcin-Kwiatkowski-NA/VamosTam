package com.blablatwo.ride.dto;

import com.blablatwo.city.CityDto;

import java.time.LocalDateTime;

public record RideStopDto(
        int stopOrder,
        CityDto city,
        LocalDateTime departureTime
) {
}

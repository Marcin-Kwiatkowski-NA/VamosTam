package com.vamigo.ride.event;

import com.vamigo.ride.dto.RideResponseDto;

public record ExternalRideCreatedEvent(
        RideResponseDto ride,
        String sourceUrl
) {}

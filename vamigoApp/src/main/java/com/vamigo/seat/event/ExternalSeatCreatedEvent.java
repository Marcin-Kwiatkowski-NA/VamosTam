package com.vamigo.seat.event;

import com.vamigo.seat.dto.SeatResponseDto;

public record ExternalSeatCreatedEvent(
        SeatResponseDto seat,
        String sourceUrl
) {}

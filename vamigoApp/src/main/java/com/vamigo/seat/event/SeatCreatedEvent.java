package com.vamigo.seat.event;

import com.vamigo.seat.dto.SeatResponseDto;

public record SeatCreatedEvent(
        Long seatId,
        Long userId,
        SeatResponseDto seat
) {
}

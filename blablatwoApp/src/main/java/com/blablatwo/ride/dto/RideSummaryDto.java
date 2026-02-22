package com.blablatwo.ride.dto;

import com.blablatwo.dto.UserCardDto;
import com.blablatwo.location.LocationDto;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder(toBuilder = true)
public record RideSummaryDto(
        Long id,
        LocationDto origin,
        LocationDto destination,
        LocalDateTime departureTime,
        BigDecimal pricePerSeat,
        int totalSeats,
        UserCardDto driver
) {
}

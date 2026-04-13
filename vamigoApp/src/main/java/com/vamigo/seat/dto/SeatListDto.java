package com.vamigo.seat.dto;

import com.vamigo.domain.Currency;
import com.vamigo.domain.TimePrecision;
import com.vamigo.dto.UserCardDto;
import com.vamigo.location.LocationDto;
import com.vamigo.ride.RideSource;
import com.vamigo.seat.SeatStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder(toBuilder = true)
public record SeatListDto(
        Long id,
        RideSource source,
        LocationDto origin,
        LocationDto destination,
        Instant departureTime,
        TimePrecision timePrecision,
        int count,
        BigDecimal priceWillingToPay,
        UserCardDto passenger,
        SeatStatus seatStatus,
        Currency currency
) {
}

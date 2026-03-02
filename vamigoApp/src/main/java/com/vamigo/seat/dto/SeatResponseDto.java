package com.vamigo.seat.dto;

import com.vamigo.dto.ContactMethodDto;
import com.vamigo.dto.UserCardDto;
import com.vamigo.location.LocationDto;
import com.vamigo.ride.RideSource;
import com.vamigo.seat.SeatStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder(toBuilder = true)
public record SeatResponseDto(
        Long id,
        RideSource source,
        LocationDto origin,
        LocationDto destination,
        Instant departureTime,
        boolean isTimeApproximate,
        int count,
        BigDecimal priceWillingToPay,
        String description,
        UserCardDto passenger,
        List<ContactMethodDto> contactMethods,
        SeatStatus seatStatus
) {
}

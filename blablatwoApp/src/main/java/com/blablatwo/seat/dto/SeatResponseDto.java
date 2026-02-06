package com.blablatwo.seat.dto;

import com.blablatwo.city.CityDto;
import com.blablatwo.dto.ContactMethodDto;
import com.blablatwo.dto.UserCardDto;
import com.blablatwo.ride.RideSource;
import com.blablatwo.seat.SeatStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
public record SeatResponseDto(
        Long id,
        RideSource source,
        CityDto origin,
        CityDto destination,
        LocalDateTime departureTime,
        boolean isApproximate,
        int count,
        BigDecimal priceWillingToPay,
        String description,
        UserCardDto passenger,
        List<ContactMethodDto> contactMethods,
        SeatStatus seatStatus
) {
}

package com.vamigo.ride.dto;

import com.vamigo.domain.Currency;
import com.vamigo.domain.TimePrecision;
import com.vamigo.dto.UserCardDto;
import com.vamigo.location.LocationDto;
import com.vamigo.ride.RideSource;
import com.vamigo.ride.RideStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder(toBuilder = true)
public record RideListDto(
        Long id,
        RideSource source,
        LocationDto origin,
        LocationDto destination,
        List<RideStopDto> stops,
        Instant departureTime,
        TimePrecision timePrecision,
        BigDecimal pricePerSeat,
        int availableSeats,
        int seatsTaken,
        int totalSeats,
        boolean autoApprove,
        boolean doorToDoor,
        boolean acceptsPackages,
        UserCardDto driver,
        RideStatus rideStatus,
        Currency currency,
        boolean bookingEnabled
) {
}

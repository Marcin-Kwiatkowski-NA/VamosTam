package com.vamigo.ride;

import com.vamigo.domain.Currency;
import com.vamigo.domain.TimePrecision;

import java.math.BigDecimal;
import java.time.Instant;

public record RideDetails(
        Instant departureTime,
        TimePrecision timePrecision,
        int totalSeats,
        BigDecimal pricePerSeat,
        boolean autoApprove,
        boolean doorToDoor,
        boolean acceptsPackages,
        String description,
        String contactPhone,
        Currency currency
) {
}

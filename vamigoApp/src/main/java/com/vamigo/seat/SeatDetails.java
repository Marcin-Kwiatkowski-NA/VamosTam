package com.vamigo.seat;

import com.vamigo.domain.Currency;
import com.vamigo.domain.TimePrecision;
import com.vamigo.location.Location;

import java.math.BigDecimal;
import java.time.Instant;

public record SeatDetails(
        Location origin,
        Location destination,
        Instant departureTime,
        TimePrecision timePrecision,
        int count,
        BigDecimal priceWillingToPay,
        String description,
        String contactPhone,
        Currency currency
) {
}

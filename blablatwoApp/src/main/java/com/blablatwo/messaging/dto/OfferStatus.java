package com.blablatwo.messaging.dto;

import com.blablatwo.ride.RideStatus;
import com.blablatwo.seat.SeatStatus;

/**
 * Unified offer status covering both ride and seat statuses.
 * Values map 1:1 to the existing {@link RideStatus} and {@link SeatStatus} enums.
 */
public enum OfferStatus {
    OPEN,
    FULL,
    COMPLETED,
    SEARCHING,
    BOOKED,
    EXPIRED,
    CANCELLED,
    BANNED;

    public static OfferStatus from(RideStatus s) {
        return OfferStatus.valueOf(s.name());
    }

    public static OfferStatus from(SeatStatus s) {
        return OfferStatus.valueOf(s.name());
    }
}

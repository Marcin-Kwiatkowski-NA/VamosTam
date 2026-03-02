package com.vamigo.ride;

import java.util.Map;
import java.util.Set;

public enum BookingStatus {

    PENDING,
    CONFIRMED,
    REJECTED,
    CANCELLED_BY_PASSENGER,
    CANCELLED_BY_DRIVER,
    EXPIRED;

    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED_TRANSITIONS = Map.of(
            PENDING, Set.of(CONFIRMED, REJECTED, CANCELLED_BY_PASSENGER, CANCELLED_BY_DRIVER, EXPIRED),
            CONFIRMED, Set.of(CANCELLED_BY_PASSENGER, CANCELLED_BY_DRIVER)
    );

    public boolean canTransitionTo(BookingStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isActive() {
        return this == PENDING || this == CONFIRMED;
    }
}

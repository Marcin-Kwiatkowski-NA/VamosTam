package com.vamigo.exceptions;

import com.vamigo.ride.BookingStatus;

public class InvalidBookingTransitionException extends RuntimeException {
    public InvalidBookingTransitionException(Long bookingId, BookingStatus from, BookingStatus to) {
        super("Cannot transition booking " + bookingId + " from " + from + " to " + to);
    }
}

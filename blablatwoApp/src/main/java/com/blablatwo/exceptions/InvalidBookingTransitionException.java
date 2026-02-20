package com.blablatwo.exceptions;

import com.blablatwo.ride.BookingStatus;

public class InvalidBookingTransitionException extends RuntimeException {
    public InvalidBookingTransitionException(Long bookingId, BookingStatus from, BookingStatus to) {
        super("Cannot transition booking " + bookingId + " from " + from + " to " + to);
    }
}

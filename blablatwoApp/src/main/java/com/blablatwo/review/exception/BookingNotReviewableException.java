package com.blablatwo.review.exception;

public class BookingNotReviewableException extends RuntimeException {
    public BookingNotReviewableException(Long bookingId, String reason) {
        super("Booking " + bookingId + " is not reviewable: " + reason);
    }
}

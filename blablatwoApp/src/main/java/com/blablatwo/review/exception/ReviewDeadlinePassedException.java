package com.blablatwo.review.exception;

public class ReviewDeadlinePassedException extends RuntimeException {
    public ReviewDeadlinePassedException(Long bookingId) {
        super("Review deadline has passed for booking " + bookingId);
    }
}

package com.vamigo.review.exception;

public class ReviewDeadlinePassedException extends RuntimeException {
    public ReviewDeadlinePassedException(Long bookingId) {
        super("Review deadline has passed for booking " + bookingId);
    }
}

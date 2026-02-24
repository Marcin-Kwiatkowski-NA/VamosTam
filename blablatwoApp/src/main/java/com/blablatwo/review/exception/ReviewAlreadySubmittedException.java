package com.blablatwo.review.exception;

public class ReviewAlreadySubmittedException extends RuntimeException {
    public ReviewAlreadySubmittedException(Long bookingId, Long authorId) {
        super("Review already submitted for booking " + bookingId + " by user " + authorId);
    }
}

package com.vamigo.review.exception;

public class ReviewNotAllowedException extends RuntimeException {
    public ReviewNotAllowedException(String reason) {
        super("Review not allowed: " + reason);
    }
}

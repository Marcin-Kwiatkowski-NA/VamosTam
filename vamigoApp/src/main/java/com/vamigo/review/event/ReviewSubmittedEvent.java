package com.vamigo.review.event;

public record ReviewSubmittedEvent(
        Long reviewId,
        Long bookingId,
        Long authorId,
        Long subjectId,
        Long rideId
) {
}

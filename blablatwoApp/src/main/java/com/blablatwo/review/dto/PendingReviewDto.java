package com.blablatwo.review.dto;

import com.blablatwo.review.ReviewRole;
import lombok.Builder;

import java.time.Instant;

@Builder
public record PendingReviewDto(
        Long bookingId,
        Long rideId,
        String peerName,
        String peerAvatarUrl,
        ReviewRole yourRole,
        String origin,
        String destination,
        Instant departureTime,
        Instant deadlineAt,
        boolean counterpartSubmitted
) {
}

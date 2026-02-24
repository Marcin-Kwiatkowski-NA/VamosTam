package com.blablatwo.review.dto;

import com.blablatwo.review.ReviewRole;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;

@Builder
public record PendingReviewDto(
        Long bookingId,
        Long rideId,
        String peerName,
        String peerAvatarUrl,
        ReviewRole yourRole,
        String origin,
        String destination,
        LocalDate departureDate,
        Instant deadlineAt,
        boolean counterpartSubmitted
) {
}

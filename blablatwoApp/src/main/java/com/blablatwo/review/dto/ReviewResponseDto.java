package com.blablatwo.review.dto;

import com.blablatwo.review.ReviewRole;
import com.blablatwo.review.ReviewStatus;
import com.blablatwo.review.ReviewTag;
import lombok.Builder;

import java.time.Instant;
import java.util.Set;

@Builder
public record ReviewResponseDto(
        Long id,
        Long bookingId,
        Long authorId,
        String authorName,
        String authorAvatarUrl,
        ReviewRole authorRole,
        int stars,
        String comment,
        Set<ReviewTag> tags,
        ReviewStatus status,
        Instant createdAt,
        Instant publishedAt
) {
}

package com.vamigo.review.dto;

import com.vamigo.review.ReviewRole;
import com.vamigo.review.ReviewStatus;
import com.vamigo.review.ReviewTag;
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

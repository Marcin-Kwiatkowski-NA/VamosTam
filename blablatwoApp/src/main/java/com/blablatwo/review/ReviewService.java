package com.blablatwo.review;

import com.blablatwo.review.dto.PendingReviewDto;
import com.blablatwo.review.dto.ReviewResponseDto;
import com.blablatwo.review.dto.ReviewSummaryDto;
import com.blablatwo.review.dto.SubmitReviewRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ReviewService {

    ReviewResponseDto submitReview(Long bookingId, Long authorId, SubmitReviewRequest request);

    Page<ReviewResponseDto> getPublishedReviews(Long subjectId, int page, int size);

    ReviewSummaryDto getReviewSummary(Long subjectId);

    List<PendingReviewDto> getPendingReviews(Long userId);

    long countPendingReviews(Long userId);
}

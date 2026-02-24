package com.blablatwo.review;

import com.blablatwo.auth.AppPrincipal;
import com.blablatwo.review.dto.PendingReviewDto;
import com.blablatwo.review.dto.ReviewResponseDto;
import com.blablatwo.review.dto.SubmitReviewRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/bookings/{bookingId}/review")
    public ResponseEntity<ReviewResponseDto> submitReview(
            @PathVariable Long bookingId,
            @Valid @RequestBody SubmitReviewRequest request,
            @AuthenticationPrincipal AppPrincipal principal) {
        ReviewResponseDto review = reviewService.submitReview(bookingId, principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @GetMapping("/users/{userId}/reviews")
    public ResponseEntity<Page<ReviewResponseDto>> getPublishedReviews(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewService.getPublishedReviews(userId, page, size));
    }

    @GetMapping("/me/pending-reviews")
    public ResponseEntity<List<PendingReviewDto>> getPendingReviews(
            @AuthenticationPrincipal AppPrincipal principal) {
        return ResponseEntity.ok(reviewService.getPendingReviews(principal.userId()));
    }

    @GetMapping("/me/pending-reviews/count")
    public ResponseEntity<Map<String, Long>> countPendingReviews(
            @AuthenticationPrincipal AppPrincipal principal) {
        long count = reviewService.countPendingReviews(principal.userId());
        return ResponseEntity.ok(Map.of("count", count));
    }
}

package com.blablatwo.review.event;

import com.blablatwo.notification.EntityType;
import com.blablatwo.notification.NotificationRequest;
import com.blablatwo.notification.NotificationService;
import com.blablatwo.notification.NotificationType;
import com.blablatwo.review.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Handles review submission events — sends gamification nudge to counterpart.
 */
@Component
public class ReviewEventListener {

    private static final Logger log = LoggerFactory.getLogger(ReviewEventListener.class);

    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;

    public ReviewEventListener(ReviewRepository reviewRepository,
                                NotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.notificationService = notificationService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewSubmitted(ReviewSubmittedEvent event) {
        // Send gamification nudge to counterpart (the subject)
        try {
            boolean counterpartAlsoSubmitted = reviewRepository.existsByBookingIdAndAuthorId(
                    event.bookingId(), event.subjectId());

            notificationService.notify(NotificationRequest.builder()
                    .recipientId(event.subjectId())
                    .type(NotificationType.REVIEW_REMINDER)
                    .entityType(EntityType.RIDE)
                    .entityId(event.rideId().toString())
                    .params(Map.of(
                            "offerKey", "r-" + event.rideId(),
                            "bookingId", event.bookingId().toString(),
                            "counterpartSubmitted", "true"))
                    .collapseKey("review-nudge:" + event.bookingId() + ":" + event.subjectId())
                    .build());

            // If counterpart also submitted, notify the author
            if (counterpartAlsoSubmitted) {
                notificationService.notify(NotificationRequest.builder()
                        .recipientId(event.authorId())
                        .type(NotificationType.REVIEW_REMINDER)
                        .entityType(EntityType.RIDE)
                        .entityId(event.rideId().toString())
                        .params(Map.of(
                                "offerKey", "r-" + event.rideId(),
                                "bothSubmitted", "true"))
                        .collapseKey("review-both:" + event.bookingId() + ":" + event.authorId())
                        .build());
            }
        } catch (Exception e) {
            log.error("Failed to handle ReviewSubmittedEvent for review {}: {}",
                    event.reviewId(), e.getMessage(), e);
        }
    }
}

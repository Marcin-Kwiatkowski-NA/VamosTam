package com.vamigo.review.event;

import com.vamigo.notification.EntityType;
import com.vamigo.notification.NotificationParamsEnricher;
import com.vamigo.notification.NotificationRequest;
import com.vamigo.notification.NotificationService;
import com.vamigo.notification.NotificationType;
import com.vamigo.review.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles review submission events — sends gamification nudge to counterpart.
 */
@Component
public class ReviewEventListener {

    private static final Logger log = LoggerFactory.getLogger(ReviewEventListener.class);

    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;
    private final NotificationParamsEnricher enricher;

    public ReviewEventListener(ReviewRepository reviewRepository,
                                NotificationService notificationService,
                                NotificationParamsEnricher enricher) {
        this.reviewRepository = reviewRepository;
        this.notificationService = notificationService;
        this.enricher = enricher;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewSubmitted(ReviewSubmittedEvent event) {
        // Send gamification nudge to counterpart (the subject)
        try {
            boolean counterpartAlsoSubmitted = reviewRepository.existsByBookingIdAndAuthorId(
                    event.bookingId(), event.subjectId());

            // Enrich once — both notifications share the same ride route
            var enriched = enricher.enrichReviewReminder(event.rideId(), event.bookingId(), "true");
            Map<String, String> baseParams = enriched.toMap();

            notificationService.notify(NotificationRequest.builder()
                    .recipientId(event.subjectId())
                    .type(NotificationType.REVIEW_REMINDER)
                    .entityType(EntityType.RIDE)
                    .entityId(event.rideId().toString())
                    .params(baseParams)
                    .collapseKey("review-nudge:" + event.bookingId() + ":" + event.subjectId())
                    .build());

            // If counterpart also submitted, notify the author that both are in
            if (counterpartAlsoSubmitted) {
                var bothParams = new LinkedHashMap<>(baseParams);
                bothParams.remove("counterpartSubmitted");
                bothParams.put("bothSubmitted", "true");

                notificationService.notify(NotificationRequest.builder()
                        .recipientId(event.authorId())
                        .type(NotificationType.REVIEW_REMINDER)
                        .entityType(EntityType.RIDE)
                        .entityId(event.rideId().toString())
                        .params(bothParams)
                        .collapseKey("review-both:" + event.bookingId() + ":" + event.authorId())
                        .build());
            }
        } catch (Exception e) {
            log.error("Failed to handle ReviewSubmittedEvent for review {}: {}",
                    event.reviewId(), e.getMessage(), e);
        }
    }
}

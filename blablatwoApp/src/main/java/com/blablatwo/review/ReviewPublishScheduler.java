package com.blablatwo.review;

import com.blablatwo.notification.EntityType;
import com.blablatwo.notification.NotificationParamsEnricher;
import com.blablatwo.notification.NotificationRequest;
import com.blablatwo.notification.NotificationService;
import com.blablatwo.notification.NotificationType;
import com.blablatwo.user.UserProfile;
import com.blablatwo.user.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Publishes pending reviews whose reveal delay has elapsed.
 * Also updates UserStats (ratingSum + ratingCount) for each subject.
 */
@Component
public class ReviewPublishScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReviewPublishScheduler.class);

    private final ReviewRepository reviewRepository;
    private final UserProfileRepository userProfileRepository;
    private final NotificationService notificationService;
    private final NotificationParamsEnricher enricher;

    public ReviewPublishScheduler(ReviewRepository reviewRepository,
                                   UserProfileRepository userProfileRepository,
                                   NotificationService notificationService,
                                   NotificationParamsEnricher enricher) {
        this.reviewRepository = reviewRepository;
        this.userProfileRepository = userProfileRepository;
        this.notificationService = notificationService;
        this.enricher = enricher;
    }

    @Scheduled(cron = "${review.publish-cron}")
    @Transactional
    public void publishPendingReviews() {
        List<Review> readyToPublish = reviewRepository.findPendingReviewsReadyToPublish(Instant.now());

        if (readyToPublish.isEmpty()) return;

        log.info("Publishing {} pending reviews", readyToPublish.size());

        for (Review review : readyToPublish) {
            review.setStatus(ReviewStatus.PUBLISHED);

            // Update UserStats for the subject
            Long subjectId = review.getSubject().getId();
            UserProfile subjectProfile = userProfileRepository.findById(subjectId).orElse(null);
            if (subjectProfile != null && subjectProfile.getStats() != null) {
                subjectProfile.getStats().setRatingSum(
                        subjectProfile.getStats().getRatingSum() + review.getStars());
                subjectProfile.getStats().setRatingCount(
                        subjectProfile.getStats().getRatingCount() + 1);
            }

            // Notify the subject that a review was published
            try {
                var enriched = enricher.enrichReviewReceived(review.getId(), subjectId);
                var params = new java.util.LinkedHashMap<>(enriched.toMap());
                params.put("reviewId", review.getId().toString());

                notificationService.notify(NotificationRequest.builder()
                        .recipientId(subjectId)
                        .type(NotificationType.REVIEW_RECEIVED)
                        .entityType(EntityType.REVIEW)
                        .entityId(review.getId().toString())
                        .params(params)
                        .collapseKey("review-received:" + subjectId)
                        .build());
            } catch (Exception e) {
                log.error("Failed to notify subject {} about published review {}: {}",
                        subjectId, review.getId(), e.getMessage(), e);
            }
        }
    }
}

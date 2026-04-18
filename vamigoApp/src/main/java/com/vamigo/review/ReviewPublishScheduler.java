package com.vamigo.review;

import com.vamigo.notification.EntityType;
import com.vamigo.notification.NotificationParamsEnricher;
import com.vamigo.notification.NotificationRequest;
import com.vamigo.notification.NotificationService;
import com.vamigo.notification.NotificationType;
import com.vamigo.notification.TargetType;
import com.vamigo.user.UserProfile;
import com.vamigo.user.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

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
                        .entityId(subjectId.toString())
                        .targetType(TargetType.ENTITY)
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

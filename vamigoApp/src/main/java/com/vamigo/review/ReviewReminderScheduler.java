package com.vamigo.review;

import com.vamigo.domain.Status;
import com.vamigo.messaging.SystemMessageService;
import com.vamigo.notification.EntityType;
import com.vamigo.notification.NotificationParamsEnricher;
import com.vamigo.notification.NotificationRequest;
import com.vamigo.notification.NotificationService;
import com.vamigo.notification.NotificationType;
import com.vamigo.notification.TargetType;
import com.vamigo.ride.Ride;
import com.vamigo.ride.RideBooking;
import com.vamigo.ride.RideBookingRepository;
import com.vamigo.ride.RideRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Day 2-3 review nudge (touchpoint 2).
 * Touchpoint 1 is immediate on ride completion (handled by RideCompletionListener).
 */
@Component
public class ReviewReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReviewReminderScheduler.class);

    private final RideRepository rideRepository;
    private final RideBookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewReminderTrackingRepository reminderTrackingRepository;
    private final NotificationService notificationService;
    private final SystemMessageService systemMessageService;
    private final NotificationParamsEnricher enricher;

    public ReviewReminderScheduler(RideRepository rideRepository,
                                    RideBookingRepository bookingRepository,
                                    ReviewRepository reviewRepository,
                                    ReviewReminderTrackingRepository reminderTrackingRepository,
                                    NotificationService notificationService,
                                    SystemMessageService systemMessageService,
                                    NotificationParamsEnricher enricher) {
        this.rideRepository = rideRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.reminderTrackingRepository = reminderTrackingRepository;
        this.notificationService = notificationService;
        this.systemMessageService = systemMessageService;
        this.enricher = enricher;
    }

    @Scheduled(cron = "${review.reminder-cron}")
    @Transactional
    public void sendReviewReminders() {
        Instant from = Instant.now().minus(72, ChronoUnit.HOURS);
        Instant to = Instant.now().minus(48, ChronoUnit.HOURS);

        List<Ride> completedRides = rideRepository.findByStatusAndCompletedAtBetween(Status.COMPLETED, from, to);

        if (completedRides.isEmpty()) return;

        int sent = 0;

        for (Ride ride : completedRides) {
            List<RideBooking> confirmedBookings = ride.getConfirmedBookings();

            for (RideBooking booking : confirmedBookings) {
                // Check both driver and passenger
                sent += sendReminderIfNeeded(booking, ride.getDriver().getId(), booking.getPassenger().getId(), ride);
                sent += sendReminderIfNeeded(booking, booking.getPassenger().getId(), ride.getDriver().getId(), ride);
            }
        }

        if (sent > 0) {
            log.info("Sent {} review reminder nudges", sent);
        }
    }

    private int sendReminderIfNeeded(RideBooking booking, Long userId, Long peerId, Ride ride) {
        // Skip if already reviewed
        if (reviewRepository.existsByBookingIdAndAuthorId(booking.getId(), userId)) {
            return 0;
        }

        // Skip if NUDGE already sent
        if (reminderTrackingRepository.existsByBookingIdAndUserIdAndType(
                booking.getId(), userId, ReviewReminderTracking.ReminderType.NUDGE)) {
            return 0;
        }

        // Record the reminder
        reminderTrackingRepository.save(ReviewReminderTracking.builder()
                .bookingId(booking.getId())
                .userId(userId)
                .type(ReviewReminderTracking.ReminderType.NUDGE)
                .sentAt(Instant.now())
                .build());

        // Check if counterpart submitted (for gamification text)
        boolean counterpartSubmitted = reviewRepository.existsByBookingIdAndAuthorId(
                booking.getId(), peerId);

        // Send push notification
        try {
            var enriched = enricher.enrichReviewReminder(
                    ride.getId(), booking.getId(), String.valueOf(counterpartSubmitted));
            notificationService.notify(NotificationRequest.builder()
                    .recipientId(userId)
                    .type(NotificationType.REVIEW_REMINDER)
                    .entityType(EntityType.RIDE)
                    .entityId(ride.getId().toString())
                    .targetType(TargetType.ENTITY)
                    .params(enriched.toMap())
                    .collapseKey("review-reminder:" + booking.getId() + ":" + userId)
                    .build());
        } catch (Exception e) {
            log.error("Failed to send review reminder for booking {} user {}: {}",
                    booking.getId(), userId, e.getMessage(), e);
        }

        // Post silent system message in the conversation
        try {
            String topicKey = "offer:r-" + ride.getId();
            systemMessageService.postSystemMessageToConversation(
                    topicKey, userId, peerId, "system.review_reminder", true);
        } catch (Exception e) {
            log.error("Failed to post review reminder system message for booking {} user {}: {}",
                    booking.getId(), userId, e.getMessage(), e);
        }

        return 1;
    }
}

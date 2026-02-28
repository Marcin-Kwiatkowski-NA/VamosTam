package com.blablatwo.ride.event;

import com.blablatwo.messaging.SystemMessageService;
import com.blablatwo.notification.EntityType;
import com.blablatwo.notification.NotificationParamsEnricher;
import com.blablatwo.notification.NotificationRequest;
import com.blablatwo.notification.NotificationService;
import com.blablatwo.notification.NotificationType;
import com.blablatwo.ride.RideBooking;
import com.blablatwo.ride.RideBookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Handles ride completion events — sends notifications and system messages
 * to all confirmed passengers.
 */
@Component
public class RideCompletionListener {

    private static final Logger log = LoggerFactory.getLogger(RideCompletionListener.class);

    private final RideBookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final SystemMessageService systemMessageService;
    private final NotificationParamsEnricher enricher;

    public RideCompletionListener(RideBookingRepository bookingRepository,
                                   NotificationService notificationService,
                                   SystemMessageService systemMessageService,
                                   NotificationParamsEnricher enricher) {
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
        this.systemMessageService = systemMessageService;
        this.enricher = enricher;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRideCompleted(RideCompletedEvent event) {
        // Post system message to all conversations for this ride
        try {
            String topicKey = "offer:r-" + event.rideId();
            systemMessageService.postSystemMessage(topicKey, event.driverId(), "system.ride_completed");
        } catch (Exception e) {
            log.error("Failed to post ride completed system message for ride {}: {}",
                    event.rideId(), e.getMessage(), e);
        }

        // Enrich ONCE, reuse for all notifications
        var enriched = enricher.enrichRideCompleted(event.rideId());
        Map<String, String> params = enriched.toMap();

        // Send RIDE_COMPLETED notification to each confirmed passenger
        for (Long bookingId : event.confirmedBookingIds()) {
            try {
                RideBooking booking = bookingRepository.findById(bookingId).orElse(null);
                if (booking == null) continue;

                notificationService.notify(NotificationRequest.builder()
                        .recipientId(booking.getPassenger().getId())
                        .type(NotificationType.RIDE_COMPLETED)
                        .entityType(EntityType.RIDE)
                        .entityId(event.rideId().toString())
                        .params(params)
                        .collapseKey("ride-completed:" + event.rideId())
                        .build());
            } catch (Exception e) {
                log.error("Failed to notify passenger for booking {}: {}",
                        bookingId, e.getMessage(), e);
            }
        }

        // Notify the driver as well
        try {
            notificationService.notify(NotificationRequest.builder()
                    .recipientId(event.driverId())
                    .type(NotificationType.RIDE_COMPLETED)
                    .entityType(EntityType.RIDE)
                    .entityId(event.rideId().toString())
                    .params(params)
                    .collapseKey("ride-completed:" + event.rideId())
                    .build());
        } catch (Exception e) {
            log.error("Failed to notify driver {} for ride completion: {}",
                    event.driverId(), e.getMessage(), e);
        }
    }
}

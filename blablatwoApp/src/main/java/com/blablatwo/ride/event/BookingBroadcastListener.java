package com.blablatwo.ride.event;

import com.blablatwo.domain.PersonDisplayNameResolver;
import com.blablatwo.messaging.SystemMessageService;
import com.blablatwo.ride.Ride;
import com.blablatwo.ride.RideBooking;
import com.blablatwo.ride.RideBookingRepository;
import com.blablatwo.ride.dto.BookingNotificationDto;
import com.blablatwo.user.UserProfile;
import com.blablatwo.user.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Broadcasts booking events over STOMP and posts system messages.
 * Push notification dispatch is handled separately by
 * {@link com.blablatwo.notification.NotificationEventListener}.
 */
@Component
public class BookingBroadcastListener {

    private static final Logger log = LoggerFactory.getLogger(BookingBroadcastListener.class);
    private static final String BOOKING_QUEUE = "/queue/bookings";

    private final SimpMessagingTemplate messagingTemplate;
    private final RideBookingRepository bookingRepository;
    private final UserProfileRepository userProfileRepository;
    private final PersonDisplayNameResolver displayNameResolver;
    private final SystemMessageService systemMessageService;

    public BookingBroadcastListener(SimpMessagingTemplate messagingTemplate,
                                     RideBookingRepository bookingRepository,
                                     UserProfileRepository userProfileRepository,
                                     PersonDisplayNameResolver displayNameResolver,
                                     SystemMessageService systemMessageService) {
        this.messagingTemplate = messagingTemplate;
        this.bookingRepository = bookingRepository;
        this.userProfileRepository = userProfileRepository;
        this.displayNameResolver = displayNameResolver;
        this.systemMessageService = systemMessageService;
    }

    @AsyncAfterCommitListener
    public void onBookingRequested(BookingRequestedEvent event) {
        broadcastStomp(event.bookingId(), event.driverId(), event.passengerId(), "REQUESTED");
    }

    @AsyncAfterCommitListener
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        broadcastStomp(event.bookingId(), event.passengerId(), event.driverId(), "CONFIRMED");
        postSystemMessage(event.rideId(), event.driverId(), "system.booking_confirmed");
    }

    @AsyncAfterCommitListener
    public void onBookingRejected(BookingRejectedEvent event) {
        broadcastStomp(event.bookingId(), event.passengerId(), event.driverId(), "REJECTED");
        postSystemMessage(event.rideId(), event.driverId(), "system.booking_rejected");
    }

    @AsyncAfterCommitListener
    public void onBookingCancelled(BookingCancelledEvent event) {
        Long recipientId = event.cancelledByUserId().equals(event.driverId())
                ? event.passengerId()
                : event.driverId();
        broadcastStomp(event.bookingId(), recipientId, event.cancelledByUserId(),
                "CANCELLED", event.reason());
        postSystemMessage(event.rideId(), event.cancelledByUserId(), "system.booking_cancelled");
    }

    @AsyncAfterCommitListener
    public void onBookingExpired(BookingExpiredEvent event) {
        broadcastStomp(event.bookingId(), event.passengerId(), event.driverId(), "EXPIRED");
    }

    private void postSystemMessage(Long rideId, Long actorId, String bodyKey) {
        try {
            String topicKey = "offer:r-" + rideId;
            systemMessageService.postSystemMessage(topicKey, actorId, bodyKey);
        } catch (Exception e) {
            log.error("Failed to post system message for ride {}: {}", rideId, e.getMessage(), e);
        }
    }

    private void broadcastStomp(Long bookingId, Long recipientId, Long counterpartyId,
                                 String eventType) {
        broadcastStomp(bookingId, recipientId, counterpartyId, eventType, null);
    }

    private void broadcastStomp(Long bookingId, Long recipientId, Long counterpartyId,
                                 String eventType, String cancellationReason) {
        try {
            RideBooking booking = bookingRepository.findByIdWithRideAndLocations(bookingId).orElse(null);
            if (booking == null) {
                log.warn("Booking {} not found for broadcast", bookingId);
                return;
            }

            Ride ride = booking.getRide();
            String counterpartyName = resolveDisplayName(counterpartyId);

            BookingNotificationDto payload = BookingNotificationDto.builder()
                    .bookingId(bookingId)
                    .rideId(ride.getId())
                    .status(booking.getStatus())
                    .eventType(eventType)
                    .seatCount(booking.getSeatCount())
                    .rideOrigin(ride.getOrigin().getName(null))
                    .rideDestination(ride.getDestination().getName(null))
                    .departureTime(ride.getDepartureTime())
                    .counterpartyName(counterpartyName)
                    .cancellationReason(cancellationReason)
                    .build();

            messagingTemplate.convertAndSendToUser(
                    recipientId.toString(), BOOKING_QUEUE, payload);

        } catch (Exception e) {
            log.error("Failed to broadcast booking event {} for booking {}: {}",
                    eventType, bookingId, e.getMessage(), e);
        }
    }

    private String resolveDisplayName(Long userId) {
        try {
            UserProfile profile = userProfileRepository.findById(userId).orElse(null);
            return displayNameResolver.resolveInternal(profile, userId);
        } catch (Exception e) {
            return "User";
        }
    }
}

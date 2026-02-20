package com.blablatwo.ride.event;

import com.blablatwo.domain.PersonDisplayNameResolver;
import com.blablatwo.notification.PushNotificationService;
import com.blablatwo.ride.BookingStatus;
import com.blablatwo.ride.Ride;
import com.blablatwo.ride.RideBooking;
import com.blablatwo.ride.RideBookingRepository;
import com.blablatwo.ride.dto.BookingNotificationDto;
import com.blablatwo.user.UserProfile;
import com.blablatwo.user.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
public class BookingBroadcastListener {

    private static final Logger log = LoggerFactory.getLogger(BookingBroadcastListener.class);
    private static final String BOOKING_QUEUE = "/queue/bookings";

    private final SimpMessagingTemplate messagingTemplate;
    private final PushNotificationService pushNotificationService;
    private final RideBookingRepository bookingRepository;
    private final UserProfileRepository userProfileRepository;
    private final PersonDisplayNameResolver displayNameResolver;

    public BookingBroadcastListener(SimpMessagingTemplate messagingTemplate,
                                     PushNotificationService pushNotificationService,
                                     RideBookingRepository bookingRepository,
                                     UserProfileRepository userProfileRepository,
                                     PersonDisplayNameResolver displayNameResolver) {
        this.messagingTemplate = messagingTemplate;
        this.pushNotificationService = pushNotificationService;
        this.bookingRepository = bookingRepository;
        this.userProfileRepository = userProfileRepository;
        this.displayNameResolver = displayNameResolver;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingRequested(BookingRequestedEvent event) {
        notifyUser(event.bookingId(), event.driverId(), event.passengerId(),
                "REQUESTED", "New booking request",
                "%s wants to join your ride");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        notifyUser(event.bookingId(), event.passengerId(), event.driverId(),
                "CONFIRMED", "Booking confirmed",
                "%s confirmed your booking");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingRejected(BookingRejectedEvent event) {
        notifyUser(event.bookingId(), event.passengerId(), event.driverId(),
                "REJECTED", "Booking rejected",
                "%s rejected your booking request");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCancelled(BookingCancelledEvent event) {
        Long recipientId = event.cancelledByUserId().equals(event.driverId())
                ? event.passengerId()
                : event.driverId();
        notifyUser(event.bookingId(), recipientId, event.cancelledByUserId(),
                "CANCELLED", "Booking cancelled",
                "%s cancelled the booking");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingExpired(BookingExpiredEvent event) {
        notifyUser(event.bookingId(), event.passengerId(), event.driverId(),
                "EXPIRED", "Booking expired",
                "Your booking request expired");
    }

    private void notifyUser(Long bookingId, Long recipientId, Long counterpartyId,
                             String eventType, String pushTitle, String pushBodyTemplate) {
        try {
            RideBooking booking = bookingRepository.findById(bookingId).orElse(null);
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
                    .departureTime(ride.getDepartureDateTime())
                    .counterpartyName(counterpartyName)
                    .build();

            messagingTemplate.convertAndSendToUser(
                    recipientId.toString(), BOOKING_QUEUE, payload);

            String pushBody = pushBodyTemplate.contains("%s")
                    ? String.format(pushBodyTemplate, counterpartyName)
                    : pushBodyTemplate;

            pushNotificationService.sendToUser(recipientId, pushTitle, pushBody,
                    Map.of("type", "booking",
                            "bookingId", bookingId.toString(),
                            "rideId", ride.getId().toString(),
                            "eventType", eventType));

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

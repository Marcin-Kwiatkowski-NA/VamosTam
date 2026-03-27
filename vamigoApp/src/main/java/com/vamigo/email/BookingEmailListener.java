package com.vamigo.email;

import com.vamigo.domain.PersonDisplayNameResolver;
import com.vamigo.ride.Ride;
import com.vamigo.ride.RideBooking;
import com.vamigo.ride.RideBookingRepository;
import com.vamigo.ride.event.BookingConfirmedEvent;
import com.vamigo.ride.event.BookingRequestedEvent;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import com.vamigo.user.UserProfile;
import com.vamigo.user.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class BookingEmailListener {

    private static final Logger log = LoggerFactory.getLogger(BookingEmailListener.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Warsaw"));

    private final BrevoClient brevoClient;
    private final RideBookingRepository bookingRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final PersonDisplayNameResolver displayNameResolver;
    private final long bookingRequestedTemplateId;
    private final long bookingRequestedTemplateIdPl;
    private final long bookingConfirmedTemplateId;
    private final long bookingConfirmedTemplateIdPl;

    public BookingEmailListener(BrevoClient brevoClient,
                                RideBookingRepository bookingRepository,
                                UserAccountRepository userAccountRepository,
                                UserProfileRepository userProfileRepository,
                                PersonDisplayNameResolver displayNameResolver,
                                @Value("${brevo.booking-requested-template-id}") long bookingRequestedTemplateId,
                                @Value("${brevo.booking-requested-template-id-pl}") long bookingRequestedTemplateIdPl,
                                @Value("${brevo.booking-confirmed-template-id}") long bookingConfirmedTemplateId,
                                @Value("${brevo.booking-confirmed-template-id-pl}") long bookingConfirmedTemplateIdPl) {
        this.brevoClient = brevoClient;
        this.bookingRepository = bookingRepository;
        this.userAccountRepository = userAccountRepository;
        this.userProfileRepository = userProfileRepository;
        this.displayNameResolver = displayNameResolver;
        this.bookingRequestedTemplateId = bookingRequestedTemplateId;
        this.bookingRequestedTemplateIdPl = bookingRequestedTemplateIdPl;
        this.bookingConfirmedTemplateId = bookingConfirmedTemplateId;
        this.bookingConfirmedTemplateIdPl = bookingConfirmedTemplateIdPl;
    }

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingRequested(BookingRequestedEvent event) {
        try {
            RideBooking booking = bookingRepository.findByIdWithRideAndLocations(event.bookingId()).orElse(null);
            if (booking == null) {
                log.warn("Booking {} not found for email notification", event.bookingId());
                return;
            }

            UserAccount driver = userAccountRepository.findById(event.driverId()).orElse(null);
            if (driver == null) return;

            String passengerName = resolveDisplayName(event.passengerId());
            Ride ride = booking.getRide();
            String origin = ride.getOrigin().getName(null);
            String destination = ride.getDestination().getName(null);
            String departureTime = DATE_FMT.format(ride.getDepartureTime());

            Map<String, String> params = new LinkedHashMap<>();
            params.put("PASSENGER_NAME", passengerName);
            params.put("ORIGIN", origin);
            params.put("DESTINATION", destination);
            params.put("DEPARTURE_TIME", departureTime);
            params.put("SEAT_COUNT", String.valueOf(booking.getSeatCount()));
            params.put("DEEP_LINK", "/my-offer/r-" + event.rideId());

            brevoClient.sendTemplateEmail(
                    driver.getEmail(),
                    resolveDisplayName(event.driverId()),
                    bookingRequestedTemplateIdPl,
                    params);

            log.info("Booking requested email sent to driver {} for booking {}", event.driverId(), event.bookingId());
        } catch (Exception e) {
            log.error("Failed to send booking-requested email for booking {}: {}", event.bookingId(), e.getMessage());
        }
    }

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        try {
            RideBooking booking = bookingRepository.findByIdWithRideAndLocations(event.bookingId()).orElse(null);
            if (booking == null) {
                log.warn("Booking {} not found for email notification", event.bookingId());
                return;
            }

            UserAccount passenger = userAccountRepository.findById(event.passengerId()).orElse(null);
            if (passenger == null) return;

            String driverName = resolveDisplayName(event.driverId());
            Ride ride = booking.getRide();
            String origin = ride.getOrigin().getName(null);
            String destination = ride.getDestination().getName(null);
            String departureTime = DATE_FMT.format(ride.getDepartureTime());

            Map<String, String> params = new LinkedHashMap<>();
            params.put("DRIVER_NAME", driverName);
            params.put("ORIGIN", origin);
            params.put("DESTINATION", destination);
            params.put("DEPARTURE_TIME", departureTime);
            params.put("SEAT_COUNT", String.valueOf(booking.getSeatCount()));
            params.put("DEEP_LINK", "/offer/r-" + event.rideId());

            brevoClient.sendTemplateEmail(
                    passenger.getEmail(),
                    resolveDisplayName(event.passengerId()),
                    bookingConfirmedTemplateIdPl,
                    params);

            log.info("Booking confirmed email sent to passenger {} for booking {}", event.passengerId(), event.bookingId());
        } catch (Exception e) {
            log.error("Failed to send booking-confirmed email for booking {}: {}", event.bookingId(), e.getMessage());
        }
    }

    private String resolveDisplayName(Long userId) {
        UserProfile profile = userProfileRepository.findById(userId).orElse(null);
        return displayNameResolver.resolveInternal(profile, userId);
    }
}

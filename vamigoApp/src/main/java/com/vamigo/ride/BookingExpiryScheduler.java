package com.vamigo.ride;

import com.vamigo.ride.event.BookingExpiredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class BookingExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryScheduler.class);

    private final RideBookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${booking.pending-ttl-minutes}")
    private int pendingTtlMinutes;

    public BookingExpiryScheduler(RideBookingRepository bookingRepository,
                                   ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelayString = "${booking.expiry-check-interval-ms}")
    @Transactional
    public void expirePendingBookings() {
        Instant cutoff = Instant.now().minus(pendingTtlMinutes, ChronoUnit.MINUTES);
        List<RideBooking> stale = bookingRepository.findByStatusAndBookedAtBefore(
                BookingStatus.PENDING, cutoff);

        if (stale.isEmpty()) return;

        log.info("Expiring {} stale PENDING bookings (TTL: {} min)", stale.size(), pendingTtlMinutes);

        for (RideBooking booking : stale) {
            booking.setStatus(BookingStatus.EXPIRED);
            booking.setResolvedAt(Instant.now());

            eventPublisher.publishEvent(new BookingExpiredEvent(
                    booking.getId(),
                    booking.getRide().getId(),
                    booking.getPassenger().getId(),
                    booking.getRide().getDriver().getId()));
        }
    }
}

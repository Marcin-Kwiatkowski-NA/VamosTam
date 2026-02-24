package com.blablatwo.ride;

import com.blablatwo.domain.Status;
import com.blablatwo.ride.event.RideCompletedEvent;
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

/**
 * Auto-completes rides that have passed the estimated arrival time + buffer.
 * Only completes rides that have confirmed bookings.
 */
@Component
public class RideCompletionScheduler {

    private static final Logger log = LoggerFactory.getLogger(RideCompletionScheduler.class);

    private final RideRepository rideRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${ride.auto-complete-buffer-minutes:720}")
    private int bufferMinutes;

    public RideCompletionScheduler(RideRepository rideRepository,
                                    ApplicationEventPublisher eventPublisher) {
        this.rideRepository = rideRepository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelay = 300_000) // every 5 minutes
    @Transactional
    public void autoCompleteRides() {
        Instant cutoff = Instant.now().minus(bufferMinutes, ChronoUnit.MINUTES);

        List<Ride> ridesToComplete = rideRepository.findActiveRidesReadyForCompletion(cutoff);

        if (ridesToComplete.isEmpty()) return;

        log.info("Auto-completing {} rides (buffer: {} min)", ridesToComplete.size(), bufferMinutes);

        for (Ride ride : ridesToComplete) {
            ride.setStatus(Status.COMPLETED);
            ride.setCompletedAt(Instant.now());
            ride.setLastModified(Instant.now());

            List<Long> confirmedBookingIds = ride.getConfirmedBookings().stream()
                    .map(RideBooking::getId)
                    .toList();

            eventPublisher.publishEvent(new RideCompletedEvent(
                    ride.getId(), ride.getDriver().getId(), confirmedBookingIds));
        }
    }
}

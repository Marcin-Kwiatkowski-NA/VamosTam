package com.vamigo.ride;

import com.vamigo.ride.event.RideCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled jobs for ride lifecycle transitions:
 * <ul>
 *   <li>Auto-complete: rides with confirmed bookings, after estimated arrival + buffer</li>
 *   <li>Auto-expire: rides with no confirmed bookings, after departure + expiry window</li>
 * </ul>
 */
@Component
@EnableConfigurationProperties(RideBusinessProperties.class)
public class RideCompletionScheduler {

    private static final Logger log = LoggerFactory.getLogger(RideCompletionScheduler.class);

    private final RideRepository rideRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RideBusinessProperties properties;
    private final Clock clock;

    public RideCompletionScheduler(RideRepository rideRepository,
                                    ApplicationEventPublisher eventPublisher,
                                    RideBusinessProperties properties,
                                    Clock clock) {
        this.rideRepository = rideRepository;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${ride.completion-check-interval-ms}")
    @Transactional
    public void autoCompleteRides() {
        Instant now = Instant.now(clock);
        Instant cutoff = now.minus(properties.autoCompleteBufferMinutes(), ChronoUnit.MINUTES);

        List<Ride> ridesToComplete = rideRepository.findActiveRidesReadyForCompletion(cutoff);

        if (ridesToComplete.isEmpty()) return;

        log.info("Auto-completing {} rides (buffer: {} min)", ridesToComplete.size(), properties.autoCompleteBufferMinutes());

        for (Ride ride : ridesToComplete) {
            ride.markCompleted(now);

            List<Long> confirmedBookingIds = ride.getConfirmedBookings().stream()
                    .map(RideBooking::getId)
                    .toList();

            eventPublisher.publishEvent(new RideCompletedEvent(
                    ride.getId(), ride.getDriver().getId(), confirmedBookingIds));
        }
    }

    @Scheduled(fixedDelayString = "${ride.completion-check-interval-ms}")
    @Transactional
    public void autoExpireRides() {
        Instant cutoff = Instant.now(clock).minus(properties.noBookingExpiryMinutes(), ChronoUnit.MINUTES);

        List<Ride> ridesToExpire = rideRepository.findActiveRidesWithNoBookingsReadyForExpiry(cutoff);

        if (ridesToExpire.isEmpty()) return;

        log.info("Auto-expiring {} rides with no bookings (window: {} min)", ridesToExpire.size(), properties.noBookingExpiryMinutes());

        for (Ride ride : ridesToExpire) {
            ride.markExpired();
        }
    }
}

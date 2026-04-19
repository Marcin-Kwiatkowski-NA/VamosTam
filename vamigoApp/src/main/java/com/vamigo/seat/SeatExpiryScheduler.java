package com.vamigo.seat;

import com.vamigo.domain.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Expires active seat requests whose departure time has passed.
 */
@Component
public class SeatExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(SeatExpiryScheduler.class);

    private final SeatRepository seatRepository;
    private final Clock clock;

    public SeatExpiryScheduler(SeatRepository seatRepository, Clock clock) {
        this.seatRepository = seatRepository;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${seat.expiry-check-interval-ms}")
    @Transactional
    public void autoExpireSeats() {
        List<Seat> seatsToExpire = seatRepository.findByStatusAndDepartureTimeBefore(
                Status.ACTIVE, Instant.now(clock));

        if (seatsToExpire.isEmpty()) return;

        log.info("Auto-expiring {} seats past departure time", seatsToExpire.size());

        for (Seat seat : seatsToExpire) {
            seat.markExpired();
        }
    }
}

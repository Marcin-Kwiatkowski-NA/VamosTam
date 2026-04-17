package com.vamigo.searchalert;

import com.vamigo.ride.event.ExternalRideCreatedEvent;
import com.vamigo.ride.event.RideCreatedEvent;
import com.vamigo.seat.event.ExternalSeatCreatedEvent;
import com.vamigo.seat.event.SeatCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens to ride/seat creation events and writes outbox matches
 * for ALL existing saved searches matching the new ride/seat.
 */
@Component
public class SearchAlertMatchListener {

    private static final Logger log = LoggerFactory.getLogger(SearchAlertMatchListener.class);

    private final SearchAlertMatcher matcher;

    public SearchAlertMatchListener(SearchAlertMatcher matcher) {
        this.matcher = matcher;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRideCreated(RideCreatedEvent event) {
        try {
            matcher.matchRide(event.ride(), event.driverId());
        } catch (Exception e) {
            log.error("Failed to match ride {} against saved searches", event.rideId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSeatCreated(SeatCreatedEvent event) {
        try {
            matcher.matchSeat(event.seat(), event.userId());
        } catch (Exception e) {
            log.error("Failed to match seat {} against saved searches", event.seatId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExternalRideCreated(ExternalRideCreatedEvent event) {
        try {
            Long driverId = event.ride().driver() != null ? event.ride().driver().id() : null;
            matcher.matchRide(event.ride(), driverId);
        } catch (Exception e) {
            log.error("Failed to match external ride {} against saved searches", event.ride().id(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExternalSeatCreated(ExternalSeatCreatedEvent event) {
        try {
            Long userId = event.seat().passenger() != null ? event.seat().passenger().id() : null;
            matcher.matchSeat(event.seat(), userId);
        } catch (Exception e) {
            log.error("Failed to match external seat {} against saved searches", event.seat().id(), e);
        }
    }
}

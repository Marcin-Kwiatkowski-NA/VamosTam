package com.vamigo.searchalert;

import com.vamigo.location.LocationDto;
import com.vamigo.ride.dto.RideResponseDto;
import com.vamigo.ride.dto.RideStopDto;
import com.vamigo.ride.event.RideCreatedEvent;
import com.vamigo.seat.dto.SeatResponseDto;
import com.vamigo.seat.event.SeatCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Auto-creates a saved search when a user creates a ride or seat,
 * so they get notified about matching counter-offers.
 * <p>
 * Ride created → auto-alert for SEAT (looking for passengers).
 * Seat created → auto-alert for RIDE (looking for rides).
 */
@Component
public class SearchAlertAutoCreateListener {

    private static final Logger log = LoggerFactory.getLogger(SearchAlertAutoCreateListener.class);

    private final SavedSearchService savedSearchService;

    public SearchAlertAutoCreateListener(SavedSearchService savedSearchService) {
        this.savedSearchService = savedSearchService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRideCreated(RideCreatedEvent event) {
        try {
            RideResponseDto ride = event.ride();
            List<RideStopDto> stops = ride.stops();
            if (stops == null || stops.size() < 2) return;

            LocalDate departureDate = ride.departureTime().atOffset(ZoneOffset.UTC).toLocalDate();

            // Create auto-alert for every valid stop pair (i, j) where i < j.
            // A ride Kraków → Katowice → Warszawa generates alerts for:
            // Kraków→Katowice, Kraków→Warszawa, Katowice→Warszawa
            for (int i = 0; i < stops.size(); i++) {
                for (int j = i + 1; j < stops.size(); j++) {
                    LocationDto origin = stops.get(i).location();
                    LocationDto dest = stops.get(j).location();

                    savedSearchService.createAutoAlert(
                            event.driverId(),
                            origin.name(), origin.osmId(), origin.latitude(), origin.longitude(),
                            dest.name(), dest.osmId(), dest.latitude(), dest.longitude(),
                            departureDate, SearchType.SEAT);
                }
            }
        } catch (Exception e) {
            log.error("Failed to auto-create alert for ride {}", event.rideId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSeatCreated(SeatCreatedEvent event) {
        try {
            SeatResponseDto seat = event.seat();
            LocationDto origin = seat.origin();
            LocationDto dest = seat.destination();
            if (origin == null || dest == null) return;

            LocalDate departureDate = seat.departureTime().atOffset(ZoneOffset.UTC).toLocalDate();

            // When a seat is created, auto-create alert for RIDE type
            // (passenger wants to know about new rides on this route)
            savedSearchService.createAutoAlert(
                    event.userId(),
                    origin.name(), origin.osmId(), origin.latitude(), origin.longitude(),
                    dest.name(), dest.osmId(), dest.latitude(), dest.longitude(),
                    departureDate, SearchType.RIDE);
        } catch (Exception e) {
            log.error("Failed to auto-create alert for seat {}", event.seatId(), e);
        }
    }
}

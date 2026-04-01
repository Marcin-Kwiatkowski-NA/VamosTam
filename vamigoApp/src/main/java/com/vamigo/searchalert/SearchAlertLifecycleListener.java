package com.vamigo.searchalert;

import com.vamigo.ride.Ride;
import com.vamigo.ride.RideRepository;
import com.vamigo.ride.RideStop;
import com.vamigo.ride.event.BookingConfirmedEvent;
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
 * Deactivates auto-created alerts when the underlying ride/seat becomes irrelevant.
 * E.g., when a ride becomes fully booked after a booking is confirmed.
 */
@Component
public class SearchAlertLifecycleListener {

    private static final Logger log = LoggerFactory.getLogger(SearchAlertLifecycleListener.class);

    private final RideRepository rideRepository;
    private final SavedSearchRepository savedSearchRepository;

    public SearchAlertLifecycleListener(RideRepository rideRepository,
                                        SavedSearchRepository savedSearchRepository) {
        this.rideRepository = rideRepository;
        this.savedSearchRepository = savedSearchRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        try {
            Ride ride = rideRepository.findById(event.rideId()).orElse(null);
            if (ride == null) return;

            // Check if ride is now full (no available seats)
            long confirmedSeats = ride.getBookings().stream()
                    .filter(b -> b.getStatus() == com.vamigo.ride.BookingStatus.CONFIRMED)
                    .mapToInt(b -> b.getSeatCount())
                    .sum();

            if (confirmedSeats >= ride.getTotalSeats()) {
                // Ride is full — delete all auto-created SEAT alerts for every stop pair
                List<RideStop> stops = ride.getStops();
                if (stops.size() >= 2) {
                    LocalDate departureDate = ride.getDepartureTime().atOffset(ZoneOffset.UTC).toLocalDate();
                    Long driverId = ride.getDriver().getId();

                    for (int i = 0; i < stops.size(); i++) {
                        for (int j = i + 1; j < stops.size(); j++) {
                            savedSearchRepository
                                    .deleteByUserIdAndOriginOsmIdAndDestinationOsmIdAndDepartureDateAndSearchTypeAndAutoCreatedTrue(
                                            driverId,
                                            stops.get(i).getLocation().getOsmId(),
                                            stops.get(j).getLocation().getOsmId(),
                                            departureDate,
                                            SearchType.SEAT);
                        }
                    }

                    log.debug("Deleted auto-alerts for fully booked ride {}", event.rideId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to process lifecycle for booking confirmed on ride {}", event.rideId(), e);
        }
    }
}

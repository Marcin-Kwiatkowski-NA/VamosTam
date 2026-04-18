package com.vamigo.ride;

import com.vamigo.AbstractFullStackTest;
import com.vamigo.domain.Status;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserProfile;
import com.vamigo.util.IntegrationFixtures;
import com.vamigo.util.TestEventCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * {@link RideCompletionScheduler#autoCompleteRides()} transitions rides with confirmed
 * bookings and elapsed {@code estimatedArrivalAt} to COMPLETED, publishing
 * {@link com.vamigo.ride.event.RideCompletedEvent}. The {@code BEFORE_COMMIT} phase of
 * {@code RideCompletionListener#updateStatsOnRideCompleted} runs synchronously before the
 * scheduler's transaction commits, so {@code ridesGiven}/{@code ridesTaken} increments are
 * visible the moment the ride status flips.
 */
@Import(TestEventCollector.class)
class RideCompletionSchedulerIT extends AbstractFullStackTest {

    @Autowired IntegrationFixtures fx;
    @Autowired TestEventCollector collector;

    @BeforeEach
    void clearCollector() {
        collector.clear();
    }

    @Test
    void rideWithConfirmedBooking_pastEstimatedArrival_isCompleted_andStatsUpdated() {
        UserAccount driver = fx.persistUser();
        UserAccount passenger = fx.persistUser();

        Ride ride = fx.persistSimpleRide(driver);
        // Force arrival into the past so the scheduler sees the ride on its next tick.
        ride.setEstimatedArrivalAt(Instant.now().minus(5, ChronoUnit.MINUTES));
        fx.rideRepository().saveAndFlush(ride);

        fx.persistBooking(ride, passenger, BookingStatus.CONFIRMED);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Ride loaded = fx.rideRepository().findById(ride.getId()).orElseThrow();
            assertThat(loaded.getStatus()).isEqualTo(Status.COMPLETED);
            assertThat(loaded.getCompletedAt()).isNotNull();
        });

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
                assertThat(collector.rideCompleted())
                        .extracting(e -> e.rideId())
                        .contains(ride.getId()));

        // BEFORE_COMMIT listener bumps stats inside the scheduler's transaction.
        UserProfile driverProfile = fx.userProfileRepository().findById(driver.getId()).orElseThrow();
        UserProfile passengerProfile = fx.userProfileRepository().findById(passenger.getId()).orElseThrow();
        assertThat(driverProfile.getStats().getRidesGiven()).isEqualTo(1);
        assertThat(passengerProfile.getStats().getRidesTaken()).isEqualTo(1);
    }
}

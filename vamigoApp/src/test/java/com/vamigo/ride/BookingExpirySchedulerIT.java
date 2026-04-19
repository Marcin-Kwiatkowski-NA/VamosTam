package com.vamigo.ride;

import com.vamigo.AbstractFullStackTest;
import com.vamigo.user.UserAccount;
import com.vamigo.util.IntegrationFixtures;
import com.vamigo.util.TestEventCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * {@link BookingExpiryScheduler} runs every 500ms in tests with {@code pending-ttl-minutes=0}
 * (see {@code application-test.properties}). A freshly seeded PENDING booking becomes
 * immediately eligible for expiry; the scheduler flips it to EXPIRED and publishes
 * {@link com.vamigo.ride.event.BookingExpiredEvent}, which {@link TestEventCollector} records.
 */
@Import(TestEventCollector.class)
class BookingExpirySchedulerIT extends AbstractFullStackTest {

    @Autowired IntegrationFixtures fx;
    @Autowired TestEventCollector collector;

    @BeforeEach
    void clearCollector() {
        collector.clear();
    }

    @Test
    void stalePendingBooking_getsExpired_andEmitsEvent() {
        UserAccount driver = fx.persistUser();
        UserAccount passenger = fx.persistUser();
        Ride ride = fx.persistSimpleRide(driver);

        // Build a PENDING booking with a stale bookedAt up front so the TTL check
        // trips on the very first scheduler tick. Going through the repository
        // directly keeps the entity encapsulated — no post-hoc setter mutation.
        RideBooking booking = fx.bookingRepository().saveAndFlush(
                RideBooking.builder()
                        .ride(ride)
                        .passenger(passenger)
                        .boardStop(ride.getStops().get(0))
                        .alightStop(ride.getStops().get(ride.getStops().size() - 1))
                        .status(BookingStatus.PENDING)
                        .seatCount(1)
                        .bookedAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                        .proposedPrice(BigDecimal.valueOf(10))
                        .build());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            RideBooking loaded = fx.bookingRepository().findById(booking.getId()).orElseThrow();
            assertThat(loaded.getStatus()).isEqualTo(BookingStatus.EXPIRED);
            assertThat(loaded.getResolvedAt()).isNotNull();
        });

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
                assertThat(collector.bookingExpired())
                        .extracting(e -> e.bookingId())
                        .contains(booking.getId()));
    }
}

package com.vamigo.ride;

import com.vamigo.AbstractFullStackTest;
import com.vamigo.notification.NotificationRepository;
import com.vamigo.ride.event.BookingConfirmedEvent;
import com.vamigo.ride.event.BookingRequestedEvent;
import com.vamigo.user.UserAccount;
import com.vamigo.util.IntegrationFixtures;
import com.vamigo.util.TestEventCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.Duration;

import static com.vamigo.util.Constants.OSM_ID_DESTINATION;
import static com.vamigo.util.Constants.OSM_ID_ORIGIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Event fan-out: primary {@code Booking*Event} are synchronous (captured by
 * {@code @RecordApplicationEvents}); secondary notifications fired from
 * {@code @TransactionalEventListener(AFTER_COMMIT)} + {@code @Async} are captured by
 * {@link TestEventCollector} and/or observed via the {@link NotificationRepository}.
 *
 * <p>{@code @RecordApplicationEvents} only sees events published inside the test's transaction
 * boundary. {@code AFTER_COMMIT} listeners fire <b>outside</b> that boundary — their secondary
 * emissions are invisible to {@code ApplicationEvents}. That's why the collector + Awaitility
 * polling the real repo is necessary for the async legs.
 */
@RecordApplicationEvents
@Import(TestEventCollector.class)
class BookingEventFanoutIT extends AbstractFullStackTest {

    @Autowired IntegrationFixtures fx;
    @Autowired ApplicationEvents events;
    @Autowired TestEventCollector collector;
    @Autowired NotificationRepository notifications;

    @BeforeEach
    void clearCollector() {
        collector.clear();
    }

    @Test
    void manualApprovalBooking_publishesRequestedEvent_andPersistsDriverNotification() {
        UserAccount driver = fx.persistUser();
        UserAccount passenger = fx.persistUser();
        Ride ride = fx.persistRideWithStops(driver, 2, /* autoApprove */ false,
                fx.persistLocation(com.vamigo.util.TestFixtures.anOriginLocation()),
                fx.persistLocation(com.vamigo.util.TestFixtures.aDestinationLocation()));

        String body = """
                {"boardStopOsmId":%d,"alightStopOsmId":%d,"seatCount":1}
                """.formatted(OSM_ID_ORIGIN, OSM_ID_DESTINATION);

        assertThat(mvc.post().uri("/rides/{rid}/bookings", ride.getId())
                .header("Authorization", fx.bearer(passenger))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .hasStatus(HttpStatus.CREATED);

        // Primary event — published synchronously inside the service transaction.
        assertThat(events.stream(BookingRequestedEvent.class).count()).isEqualTo(1);

        // Secondary fan-out — @Async @TransactionalEventListener(AFTER_COMMIT).
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(notifications.countByRecipientIdAndReadAtIsNull(driver.getId())).isPositive());

        // Also assert the collector observed the primary event (sanity of collector wiring).
        assertThat(collector.bookingRequested()).hasSize(1);
    }

    @Test
    void autoApprovedBooking_publishesConfirmedEvent_andNotifiesPassenger() {
        UserAccount driver = fx.persistUser();
        UserAccount passenger = fx.persistUser();
        Ride ride = fx.persistSimpleRide(driver);

        String body = """
                {"boardStopOsmId":%d,"alightStopOsmId":%d,"seatCount":1}
                """.formatted(OSM_ID_ORIGIN, OSM_ID_DESTINATION);

        assertThat(mvc.post().uri("/rides/{rid}/bookings", ride.getId())
                .header("Authorization", fx.bearer(passenger))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .hasStatus(HttpStatus.CREATED);

        assertThat(events.stream(BookingConfirmedEvent.class).count()).isEqualTo(1);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(notifications.countByRecipientIdAndReadAtIsNull(passenger.getId())).isPositive());

        assertThat(collector.bookingConfirmed()).hasSize(1);
    }
}

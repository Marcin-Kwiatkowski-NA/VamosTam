package com.vamigo.ride;

import com.vamigo.AbstractFullStackTest;
import com.vamigo.ride.event.BookingConfirmedEvent;
import com.vamigo.ride.event.BookingRejectedEvent;
import com.vamigo.ride.event.BookingRequestedEvent;
import com.vamigo.user.UserAccount;
import com.vamigo.util.IntegrationFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import static com.vamigo.util.Constants.OSM_ID_DESTINATION;
import static com.vamigo.util.Constants.OSM_ID_ORIGIN;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end booking lifecycle: create, confirm, reject, cancel + error paths.
 *
 * <p>{@code @RecordApplicationEvents} captures <b>primary</b> events published inside the test's
 * transaction boundary — perfect for the synchronous {@code Booking*Event} fired from
 * {@code BookingServiceImpl}. Secondary events produced by {@code @TransactionalEventListener}
 * handlers run outside the transaction and are invisible here.
 */
@RecordApplicationEvents
class BookingLifecycleIT extends AbstractFullStackTest {

    @Autowired IntegrationFixtures fx;
    @Autowired ApplicationEvents events;

    @Test
    void createAutoApprovedBooking_isConfirmed_andPublishesConfirmedEvent() {
        UserAccount driver = fx.persistUser();
        UserAccount passenger = fx.persistUser();
        Ride ride = fx.persistSimpleRide(driver);

        var result = mvc.post().uri("/rides/{rid}/bookings", ride.getId())
                .header("Authorization", fx.bearer(passenger))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookRequestJson())
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("CONFIRMED");
        assertThat(result).bodyJson().extractingPath("$.seatCount").isEqualTo(1);

        assertThat(events.stream(BookingConfirmedEvent.class).count()).isEqualTo(1);
        assertThat(events.stream(BookingRequestedEvent.class).count()).isZero();
    }

    @Test
    void createManualApprovalBooking_isPending_andPublishesRequestedEvent() {
        UserAccount driver = fx.persistUser();
        UserAccount passenger = fx.persistUser();
        Ride ride = fx.persistRideWithStops(driver, 2, /* autoApprove */ false,
                originLocation(), destinationLocation());

        assertThat(mvc.post().uri("/rides/{rid}/bookings", ride.getId())
                .header("Authorization", fx.bearer(passenger))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookRequestJson()))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().extractingPath("$.status").isEqualTo("PENDING");

        assertThat(events.stream(BookingRequestedEvent.class).count()).isEqualTo(1);
    }

    @Test
    void confirmPendingBooking_transitionsToConfirmed_andPublishesConfirmedEvent() {
        UserAccount driver = fx.persistUser();
        UserAccount passenger = fx.persistUser();
        Ride ride = fx.persistRideWithStops(driver, 2, false, originLocation(), destinationLocation());

        // create as PENDING
        assertThat(mvc.post().uri("/rides/{rid}/bookings", ride.getId())
                .header("Authorization", fx.bearer(passenger))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookRequestJson()))
                .hasStatus(HttpStatus.CREATED);

        Long bookingId = fx.bookingRepository().findByRideId(ride.getId()).getFirst().getId();

        assertThat(mvc.post().uri("/rides/{rid}/bookings/{bid}/confirm", ride.getId(), bookingId)
                .header("Authorization", fx.bearer(driver)))
                .hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("CONFIRMED");

        assertThat(events.stream(BookingConfirmedEvent.class).count()).isEqualTo(1);
    }

    @Test
    void rejectPendingBooking_transitionsToRejected_andPublishesRejectedEvent() {
        UserAccount driver = fx.persistUser();
        UserAccount passenger = fx.persistUser();
        Ride ride = fx.persistRideWithStops(driver, 2, false, originLocation(), destinationLocation());

        assertThat(mvc.post().uri("/rides/{rid}/bookings", ride.getId())
                .header("Authorization", fx.bearer(passenger))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookRequestJson()))
                .hasStatus(HttpStatus.CREATED);

        Long bookingId = fx.bookingRepository().findByRideId(ride.getId()).getFirst().getId();

        assertThat(mvc.post().uri("/rides/{rid}/bookings/{bid}/reject", ride.getId(), bookingId)
                .header("Authorization", fx.bearer(driver)))
                .hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("REJECTED");

        assertThat(events.stream(BookingRejectedEvent.class).count()).isEqualTo(1);
    }

    @Test
    void cancelConfirmedBooking_byPassenger_recordsReason() {
        UserAccount driver = fx.persistUser();
        UserAccount passenger = fx.persistUser();
        Ride ride = fx.persistSimpleRide(driver);
        RideBooking booking = fx.persistBooking(ride, passenger, BookingStatus.CONFIRMED);

        String body = """
                {"reason":"change of plans"}
                """;

        var result = mvc.post().uri("/rides/{rid}/bookings/{bid}/cancel", ride.getId(), booking.getId())
                .header("Authorization", fx.bearer(passenger))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("CANCELLED_BY_PASSENGER");
        assertThat(result).bodyJson().extractingPath("$.cancellationReason").isEqualTo("change of plans");
    }

    @Test
    void createBooking_withUnknownOsmId_returns400() {
        UserAccount driver = fx.persistUser();
        UserAccount passenger = fx.persistUser();
        Ride ride = fx.persistSimpleRide(driver);

        String body = """
                {"boardStopOsmId":9999999,"alightStopOsmId":%d,"seatCount":1}
                """.formatted(OSM_ID_DESTINATION);

        assertThat(mvc.post().uri("/rides/{rid}/bookings", ride.getId())
                .header("Authorization", fx.bearer(passenger))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void selfBooking_byDriver_isRejected() {
        UserAccount driver = fx.persistUser();
        Ride ride = fx.persistSimpleRide(driver);

        assertThat(mvc.post().uri("/rides/{rid}/bookings", ride.getId())
                .header("Authorization", fx.bearer(driver))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookRequestJson()))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    private String bookRequestJson() {
        return """
                {"boardStopOsmId":%d,"alightStopOsmId":%d,"seatCount":1}
                """.formatted(OSM_ID_ORIGIN, OSM_ID_DESTINATION);
    }

    private com.vamigo.location.Location originLocation() {
        return fx.persistLocation(com.vamigo.util.TestFixtures.anOriginLocation());
    }

    private com.vamigo.location.Location destinationLocation() {
        return fx.persistLocation(com.vamigo.util.TestFixtures.aDestinationLocation());
    }
}

package com.vamigo.ride;

import com.vamigo.AbstractFullStackTest;
import com.vamigo.user.UserAccount;
import com.vamigo.util.IntegrationFixtures;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.concurrent.StructuredTaskScope;

import static com.vamigo.util.Constants.OSM_ID_DESTINATION;
import static com.vamigo.util.Constants.OSM_ID_ORIGIN;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two threads race for the last seat via {@code POST /rides/{id}/bookings}. Exactly one 201
 * must win; the other must surface a 409 (either {@code InsufficientSeatsException} via the
 * pessimistic-lock path or {@code ObjectOptimisticLockingFailureException} mapped by the
 * framework). {@link RepeatedTest} pressure tests CI for flakes — no class-level
 * {@code @Transactional} (and neither {@code AbstractFullStackTest} nor
 * {@code AbstractIntegrationTest} annotate it) so each MockMvc call owns its own transaction.
 *
 * <p>Concurrency uses Java 25's finalized {@link StructuredTaskScope}: virtual-thread fan-out,
 * scoped lifetime, exceptions in either subtask surface from {@code Subtask.get()}.
 */
class BookingConcurrencyIT extends AbstractFullStackTest {

    @Autowired IntegrationFixtures fx;

    @RepeatedTest(20)
    void onlyOneBookingWins_theLastSeat() throws InterruptedException {
        UserAccount driver = fx.persistUser();
        UserAccount paxA = fx.persistUser();
        UserAccount paxB = fx.persistUser();
        Ride ride = fx.persistRideWithStops(driver, /* totalSeats */ 1, /* autoApprove */ true,
                fx.persistLocation(com.vamigo.util.TestFixtures.anOriginLocation()),
                fx.persistLocation(com.vamigo.util.TestFixtures.aDestinationLocation()));

        String body = """
                {"boardStopOsmId":%d,"alightStopOsmId":%d,"seatCount":1}
                """.formatted(OSM_ID_ORIGIN, OSM_ID_DESTINATION);

        int statusA;
        int statusB;
        try (var scope = StructuredTaskScope.open()) {
            StructuredTaskScope.Subtask<Integer> a =
                    scope.fork(() -> postBooking(ride.getId(), paxA, body));
            StructuredTaskScope.Subtask<Integer> b =
                    scope.fork(() -> postBooking(ride.getId(), paxB, body));
            scope.join();
            statusA = a.get();
            statusB = b.get();
        }

        assertThat(statusA == 201 ^ statusB == 201)
                .as("exactly one booking must succeed; got A=%d B=%d", statusA, statusB)
                .isTrue();
        int loser = statusA == 201 ? statusB : statusA;
        assertThat(loser)
                .as("loser must surface 409 Conflict")
                .isEqualTo(409);
    }

    private int postBooking(Long rideId, UserAccount passenger, String body) {
        return mvc.post().uri("/rides/{rid}/bookings", rideId)
                .header("Authorization", fx.bearer(passenger))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange()
                .getResponse().getStatus();
    }
}

package com.vamigo.ride;

import com.vamigo.AbstractFullStackTest;
import com.vamigo.location.Location;
import com.vamigo.user.UserAccount;
import com.vamigo.util.IntegrationFixtures;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResultAssert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Multi-stop segment capacity — exercises {@link Ride#getAvailableSeatsForSegment(int, int)} end-to-end
 * via {@link BookingController}. Four-stop ride, {@code totalSeats=3}; overlapping and disjoint bookings
 * validate per-leg accounting.
 */
class SegmentCapacityIT extends AbstractFullStackTest {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired IntegrationFixtures fx;

    @Test
    void bookingsOnDisjointSegments_bothSucceed_evenWhenSeatsEqualTotal() {
        UserAccount driver = fx.persistUser();
        UserAccount paxA = fx.persistUser();
        UserAccount paxB = fx.persistUser();

        Location a = stop(1001L, 0);
        Location b = stop(1002L, 1);
        Location c = stop(1003L, 2);
        Location d = stop(1004L, 3);
        Ride ride = fx.persistRideWithStops(driver, 3, true, a, b, c, d);

        // pax A: board A, alight B — occupies leg A→B only
        post(ride, paxA, 1001L, 1002L, 3).hasStatus(HttpStatus.CREATED);
        // pax B: board C, alight D — occupies leg C→D only; disjoint from A→B
        post(ride, paxB, 1003L, 1004L, 3).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void overlappingBookingsExceedingCapacity_areRejected() {
        UserAccount driver = fx.persistUser();
        UserAccount paxA = fx.persistUser();
        UserAccount paxB = fx.persistUser();

        Location a = stop(2001L, 0);
        Location b = stop(2002L, 1);
        Location c = stop(2003L, 2);
        Location d = stop(2004L, 3);
        Ride ride = fx.persistRideWithStops(driver, 3, true, a, b, c, d);

        // pax A: A → D — occupies all legs with 3 seats
        post(ride, paxA, 2001L, 2004L, 3).hasStatus(HttpStatus.CREATED);
        // pax B: B → C — overlaps middle leg, no capacity left
        post(ride, paxB, 2002L, 2003L, 1).hasStatus(HttpStatus.CONFLICT);
    }

    private MvcTestResultAssert post(
            Ride ride, UserAccount passenger, long boardOsm, long alightOsm, int seats) {
        String body = """
                {"boardStopOsmId":%d,"alightStopOsmId":%d,"seatCount":%d}
                """.formatted(boardOsm, alightOsm, seats);
        return assertThat(mvc.post().uri("/rides/{rid}/bookings", ride.getId())
                .header("Authorization", fx.bearer(passenger))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private Location stop(long osmId, int order) {
        return fx.persistLocation(Location.builder()
                .osmId(osmId)
                .namePl("Stop" + order)
                .nameEn("Stop" + order)
                .countryCode("PL")
                .coordinates(GF.createPoint(new Coordinate(20.0 + order, 50.0 + order))));
    }
}

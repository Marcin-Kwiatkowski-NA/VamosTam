package com.vamigo.seat;

import com.vamigo.AbstractIntegrationTest;
import com.vamigo.domain.Status;
import com.vamigo.location.Location;
import com.vamigo.user.UserAccount;
import com.vamigo.utils.PageableUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static com.vamigo.util.TestFixtures.aDestinationLocation;
import static com.vamigo.util.TestFixtures.aSeat;
import static com.vamigo.util.TestFixtures.anActiveUserAccount;
import static com.vamigo.util.TestFixtures.anOriginLocation;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies stable pagination for seats when multiple rows share the same
 * {@code departureTime}. See {@link com.vamigo.ride.RideRepositoryStableSortIT}
 * for the matching ride-side scenario.
 */
@DataJpaTest
@ActiveProfiles("test")
class SeatRepositoryStableSortIT extends AbstractIntegrationTest {

    @Autowired private TestEntityManager em;
    @Autowired private SeatRepository seatRepository;

    private UserAccount passenger;
    private Location origin;
    private Location destination;

    @BeforeEach
    void setUp() {
        origin = em.persistAndFlush(anOriginLocation().id(null).build());
        destination = em.persistAndFlush(aDestinationLocation().id(null).build());
        passenger = em.persistAndFlush(anActiveUserAccount().build());
    }

    private Seat persistSeatAt(Instant departureTime) {
        return em.persistAndFlush(aSeat(passenger, origin, destination)
                .departureTime(departureTime)
                .status(Status.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("findByPassengerIdOrderByDepartureTimeAscIdAsc breaks ties by ascending id")
    void findByPassengerIdOrderByDepartureTimeAscIdAsc_breaksTiesById() {
        Instant sharedDeparture = Instant.now().plus(1, ChronoUnit.HOURS);
        Seat first = persistSeatAt(sharedDeparture);
        Seat second = persistSeatAt(sharedDeparture);
        Seat third = persistSeatAt(sharedDeparture);
        em.clear();

        List<Seat> seats = seatRepository.findByPassengerIdOrderByDepartureTimeAscIdAsc(passenger.getId());

        assertThat(seats).extracting(Seat::getId)
                .containsExactly(first.getId(), second.getId(), third.getId());
    }

    @Test
    @DisplayName("Pagination walks every seat exactly once when departure times are identical")
    void searchPaginatesStably_whenDepartureTimesAreIdentical() {
        Instant sharedDeparture = Instant.now().plus(1, ChronoUnit.HOURS);
        List<Long> expectedIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            expectedIds.add(persistSeatAt(sharedDeparture).getId());
        }
        em.clear();

        Specification<Seat> spec = (root, query, cb) -> cb.equal(root.get("passenger"), passenger);

        List<Long> collected = new ArrayList<>();
        for (int pageNumber = 0; pageNumber < 3; pageNumber++) {
            Pageable pageable = PageableUtils.withStableSort(PageRequest.of(
                    pageNumber, 2, Sort.by(Sort.Direction.ASC, "departureTime")));
            Page<Seat> page = seatRepository.findAll(spec, pageable);
            page.getContent().forEach(s -> collected.add(s.getId()));
        }

        assertThat(collected)
                .as("all 5 seats appear exactly once across paginated queries")
                .containsExactlyElementsOf(expectedIds);
    }
}

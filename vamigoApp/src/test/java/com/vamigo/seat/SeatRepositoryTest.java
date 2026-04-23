package com.vamigo.seat;

import com.vamigo.AbstractIntegrationTest;
import com.vamigo.domain.Status;
import com.vamigo.location.Location;
import com.vamigo.user.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.vamigo.util.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class SeatRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SeatRepository repository;

    private UserAccount passenger;
    private UserAccount otherPassenger;
    private Location origin;
    private Location destination;

    @BeforeEach
    void setUp() {
        origin = em.persistAndFlush(anOriginLocation().id(null).build());
        destination = em.persistAndFlush(aDestinationLocation().id(null).build());
        passenger = em.persistAndFlush(anActiveUserAccount().build());
        otherPassenger = em.persistAndFlush(anActiveUserAccount().email("other@example.com").build());
    }

    @Nested
    @DisplayName("List a passenger's seat requests ordered by departure time")
    class FindByPassengerTests {

        @Test
        @DisplayName("Returns only seats that belong to the passenger, ordered by departure time")
        void returnsOnlyPassengersSeats() {
            em.persistAndFlush(aSeat(passenger, origin, destination)
                    .departureTime(Instant.now().plus(2, ChronoUnit.HOURS)).build());
            em.persistAndFlush(aSeat(passenger, origin, destination)
                    .departureTime(Instant.now().plus(1, ChronoUnit.HOURS)).build());
            em.persistAndFlush(aSeat(otherPassenger, origin, destination)
                    .departureTime(Instant.now().plus(3, ChronoUnit.HOURS)).build());
            em.clear();

            List<Seat> seats = repository.findByPassengerIdOrderByDepartureTimeAscIdAsc(passenger.getId());

            assertThat(seats).hasSize(2)
                    .isSortedAccordingTo((a, b) -> a.getDepartureTime().compareTo(b.getDepartureTime()));
        }
    }

    @Nested
    @DisplayName("Check passenger ownership of a seat")
    class ExistsByIdAndPassengerTests {

        @Test
        @DisplayName("Returns true when the seat belongs to the given passenger")
        void returnsTrueForOwner() {
            Seat seat = em.persistAndFlush(aSeat(passenger, origin, destination).build());
            em.clear();

            assertThat(repository.existsByIdAndPassengerId(seat.getId(), passenger.getId())).isTrue();
        }

        @Test
        @DisplayName("Returns false when the seat belongs to a different passenger")
        void returnsFalseForNonOwner() {
            Seat seat = em.persistAndFlush(aSeat(passenger, origin, destination).build());
            em.clear();

            assertThat(repository.existsByIdAndPassengerId(seat.getId(), otherPassenger.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("List seats by status with departure before a cutoff")
    class FindByStatusAndDepartureBeforeTests {

        @ParameterizedTest
        @EnumSource(value = Status.class, names = {"ACTIVE", "EXPIRED"})
        @DisplayName("Returns only seats matching the exact status for departures before the cutoff")
        void filtersByExactStatus(Status status) {
            Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
            em.persistAndFlush(aSeat(passenger, origin, destination)
                    .departureTime(past).status(status).build());
            em.clear();

            List<Seat> result = repository.findByStatusAndDepartureTimeBefore(status, Instant.now());

            assertThat(result).hasSize(1)
                    .first().extracting(Seat::getStatus).isEqualTo(status);
        }

        @Test
        @DisplayName("Excludes seats whose departure time is after the cutoff")
        void excludesSeatsWithDepartureAfterCutoff() {
            em.persistAndFlush(aSeat(passenger, origin, destination)
                    .departureTime(Instant.now().plus(1, ChronoUnit.DAYS))
                    .status(Status.ACTIVE).build());
            em.clear();

            assertThat(repository.findByStatusAndDepartureTimeBefore(Status.ACTIVE, Instant.now())).isEmpty();
        }
    }
}

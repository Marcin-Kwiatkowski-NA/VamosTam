package com.vamigo.ride;

import com.vamigo.AbstractIntegrationTest;
import com.vamigo.domain.Status;
import com.vamigo.location.Location;
import com.vamigo.location.LocationRepository;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import com.vamigo.user.UserProfileRepository;
import com.vamigo.utils.PageableUtils;
import com.vamigo.vehicle.Vehicle;
import com.vamigo.vehicle.VehicleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
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

import static com.vamigo.util.TestFixtures.aRide;
import static com.vamigo.util.TestFixtures.aTesla;
import static com.vamigo.util.TestFixtures.aUserProfile;
import static com.vamigo.util.TestFixtures.anActiveUserAccount;
import static com.vamigo.util.TestFixtures.aDestinationLocation;
import static com.vamigo.util.TestFixtures.anOriginLocation;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that paginated ride queries remain stable when several rides share the
 * same {@code departureTime}. Prior to adding the {@code id} tiebreaker, Postgres
 * was free to return tied rows in any order, causing entries to be dropped or
 * duplicated across page boundaries.
 */
@DataJpaTest
@ActiveProfiles("test")
class RideRepositoryStableSortIT extends AbstractIntegrationTest {

    @Autowired private EntityManager entityManager;
    @Autowired private RideRepository rideRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private UserProfileRepository userProfileRepository;
    @Autowired private VehicleRepository vehicleRepository;

    private UserAccount driver;
    private Vehicle vehicle;
    private Location origin;
    private Location destination;

    @BeforeEach
    void setUp() {
        origin = locationRepository.save(anOriginLocation().id(null).build());
        destination = locationRepository.save(aDestinationLocation().id(null).build());

        driver = userAccountRepository.save(anActiveUserAccount().build());
        userProfileRepository.save(aUserProfile(driver).build());

        vehicle = vehicleRepository.save(aTesla().id(null).owner(driver).build());
    }

    private Ride persistRideAt(Instant departureTime) {
        Ride ride = aRide(origin, destination)
                .id(null)
                .driver(driver)
                .vehicle(vehicle)
                .status(Status.ACTIVE)
                .departureTime(departureTime)
                .stops(new ArrayList<>())
                .build();
        ride.replaceStops(List.of(
                RideStop.builder().ride(ride).location(origin).stopOrder(0)
                        .departureTime(departureTime).build(),
                RideStop.builder().ride(ride).location(destination).stopOrder(1)
                        .departureTime(null).build()
        ));
        return rideRepository.saveAndFlush(ride);
    }

    @Test
    @DisplayName("Pagination walks every ride exactly once when departure times are identical")
    void searchPaginatesStably_whenDepartureTimesAreIdentical() {
        Instant sharedDeparture = Instant.now().plus(2, ChronoUnit.HOURS);
        List<Long> expectedIds = List.of(
                persistRideAt(sharedDeparture).getId(),
                persistRideAt(sharedDeparture).getId(),
                persistRideAt(sharedDeparture).getId(),
                persistRideAt(sharedDeparture).getId(),
                persistRideAt(sharedDeparture).getId()
        );
        entityManager.clear();

        Specification<Ride> spec = (root, query, cb) -> cb.equal(root.get("driver"), driver);

        List<Long> collected = new ArrayList<>();
        for (int pageNumber = 0; pageNumber < 3; pageNumber++) {
            Pageable pageable = PageableUtils.withStableSort(PageRequest.of(
                    pageNumber, 2, Sort.by(Sort.Direction.ASC, "departureTime")));
            Page<Ride> page = rideRepository.findAll(spec, pageable);
            page.getContent().forEach(r -> collected.add(r.getId()));
        }

        assertThat(collected)
                .as("all 5 rides appear exactly once across paginated queries")
                .containsExactlyElementsOf(expectedIds);
    }

    @Test
    @DisplayName("findByDriverIdOrderByDepartureTimeAscIdAsc breaks ties by ascending id")
    void findByDriverIdOrderByDepartureTimeAscIdAsc_breaksTiesById() {
        Instant sharedDeparture = Instant.now().plus(2, ChronoUnit.HOURS);
        Ride first = persistRideAt(sharedDeparture);
        Ride second = persistRideAt(sharedDeparture);
        Ride third = persistRideAt(sharedDeparture);
        entityManager.clear();

        List<Ride> rides = rideRepository.findByDriverIdOrderByDepartureTimeAscIdAsc(driver.getId());

        assertThat(rides).extracting(Ride::getId)
                .containsExactly(first.getId(), second.getId(), third.getId());
    }

    @Test
    @DisplayName("findByDriverIdAndStatusAndDepartureTimeAfter honors the caller-supplied stable sort")
    void findByDriverIdAndStatusAndDepartureTimeAfter_honorsCallerSort() {
        Instant sharedDeparture = Instant.now().plus(1, ChronoUnit.HOURS);
        Ride first = persistRideAt(sharedDeparture);
        Ride second = persistRideAt(sharedDeparture);
        Ride third = persistRideAt(sharedDeparture);
        entityManager.clear();

        Pageable pageable = PageableUtils.withStableSort(PageRequest.of(
                0, 10, Sort.by(Sort.Direction.ASC, "departureTime")));

        Page<Ride> page = rideRepository.findByDriverIdAndStatusAndDepartureTimeAfter(
                driver.getId(), Status.ACTIVE, Instant.now(), pageable);

        assertThat(page.getContent()).extracting(Ride::getId)
                .containsExactly(first.getId(), second.getId(), third.getId());
    }
}

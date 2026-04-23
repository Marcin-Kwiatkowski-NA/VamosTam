package com.vamigo.ride;

import com.vamigo.domain.Status;
import com.vamigo.location.Location;
import com.vamigo.location.LocationRepository;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import com.vamigo.user.UserProfileRepository;
import com.vamigo.vehicle.Vehicle;
import com.vamigo.vehicle.VehicleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import com.vamigo.AbstractIntegrationTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.vamigo.util.Constants.*;
import static com.vamigo.util.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RideRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private RideRepository rideRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private VehicleRepository vehicleRepository;

    private UserAccount driver;
    private Vehicle vehicle;
    private Location destination;
    private Location origin;

    @BeforeEach
    void setUp() {
        origin = anOriginLocation().id(null).build();
        destination = aDestinationLocation().id(null).build();
        locationRepository.save(origin);
        locationRepository.save(destination);

        driver = anActiveUserAccount().build();
        userAccountRepository.save(driver);

        userProfileRepository.save(aUserProfile(driver).build());

        vehicle = aTesla().id(null).owner(driver).build();
        vehicleRepository.save(vehicle);
    }

    private Ride createRideWithStops(Location orig, Location dest) {
        return createRideWithStops(orig, dest, FUTURE_DEPARTURE, null);
    }

    private Ride createRideWithStops(Location orig, Location dest,
                                     Instant departureTime, Instant estimatedArrival) {
        Ride ride = aRide(orig, dest)
                .id(null)
                .driver(driver)
                .vehicle(vehicle)
                .status(Status.ACTIVE)
                .departureTime(departureTime)
                .estimatedArrivalAt(estimatedArrival)
                .stops(new ArrayList<>())
                .build();
        ride.replaceStops(List.of(
                RideStop.builder().ride(ride).location(orig).stopOrder(0)
                        .departureTime(departureTime).build(),
                RideStop.builder().ride(ride).location(dest).stopOrder(1)
                        .departureTime(null).build()
        ));
        return ride;
    }

    @Test
    @DisplayName("Save a new ride with stops successfully")
    void saveNewRide() {
        Ride ride = createRideWithStops(origin, destination);

        Ride savedRide = rideRepository.save(ride);

        assertAll(
                () -> assertNotNull(savedRide.getId(), "Saved ride should have an ID"),
                () -> assertEquals(driver.getId(), savedRide.getDriver().getId(), "Driver should match"),
                () -> assertEquals(2, savedRide.getStops().size(), "Should have 2 stops"),
                () -> assertEquals(origin.getId(), savedRide.getOrigin().getId(), "Origin should match"),
                () -> assertEquals(destination.getId(), savedRide.getDestination().getId(), "Destination should match"),
                () -> assertEquals(FUTURE_DEPARTURE, savedRide.getDepartureTime(), "Departure time should match"),
                () -> assertEquals(ONE, savedRide.getTotalSeats(), "Total seats should match"),
                () -> assertEquals(BIG_DECIMAL, savedRide.getPricePerSeat(), "Price per seat should match"),
                () -> assertEquals(vehicle.getId(), savedRide.getVehicle().getId(), "Vehicle should match"),
                () -> assertEquals(Status.ACTIVE, savedRide.getStatus(), "Status should match"),
                () -> assertNotNull(savedRide.getLastModified(), "Last modified should be set by auditing"),
                () -> assertEquals(RIDE_DESCRIPTION, savedRide.getDescription(), "Description should match")
        );
    }

    @Test
    @DisplayName("Find a ride by valid ID")
    void findRideById() {
        Ride ride = createRideWithStops(origin, destination);
        Ride savedRide = rideRepository.save(ride);

        Optional<Ride> retrievedRide = rideRepository.findById(savedRide.getId());

        assertAll(
                () -> assertTrue(retrievedRide.isPresent(), "Ride should be found by ID"),
                () -> assertEquals(savedRide.getId(), retrievedRide.get().getId(), "IDs should match"),
                () -> assertEquals(Status.ACTIVE, retrievedRide.get().getStatus(), "Status should match")
        );
    }

    @Test
    @DisplayName("Update a ride's details successfully")
    void shouldUpdateRideDetails() {
        Ride ride = createRideWithStops(origin, destination);
        Ride savedRide = rideRepository.save(ride);

        Instant before = savedRide.getLastModified();
        savedRide.updateDetails(new RideDetails(
                savedRide.getDepartureTime(),
                savedRide.getTimePrecision(),
                2,
                BIG_DECIMAL.add(BigDecimal.ONE),
                savedRide.isAutoApprove(),
                savedRide.isDoorToDoor(),
                savedRide.isAcceptsPackages(),
                "Updated description",
                savedRide.getContactPhone(),
                savedRide.getCurrency()
        ));
        savedRide.cancel();

        Ride updatedRide = rideRepository.saveAndFlush(savedRide);

        assertAll(
                () -> assertEquals(2, updatedRide.getTotalSeats(), "Total seats should be updated"),
                () -> assertEquals(BIG_DECIMAL.add(BigDecimal.ONE), updatedRide.getPricePerSeat(), "Price per seat should be updated"),
                () -> assertThat(updatedRide.getLastModified())
                        .as("Last modified should advance via auditing")
                        .isAfterOrEqualTo(before),
                () -> assertEquals(Status.CANCELLED, updatedRide.getStatus(), "Status should be updated"),
                () -> assertEquals("Updated description", updatedRide.getDescription(), "Description should be updated")
        );
    }

    @Test
    @DisplayName("Delete a ride successfully")
    void shouldDeleteRide() {
        Ride ride = createRideWithStops(origin, destination);
        Ride savedRide = rideRepository.save(ride);

        rideRepository.deleteById(savedRide.getId());

        Optional<Ride> deletedRide = rideRepository.findById(savedRide.getId());
        assertFalse(deletedRide.isPresent(), "Ride should be deleted successfully");
    }

    @Test
    @DisplayName("Return empty when finding by non-existent ID")
    void returnEmptyForNonExistentId() {
        Optional<Ride> retrievedRide = rideRepository.findById(NON_EXISTENT_ID);

        assertFalse(retrievedRide.isPresent(), "No ride should be found with non-existent ID");
    }

    @Test
    @DisplayName("Find all rides when no rides exist returns empty list")
    void findAllRidesWhenNoneExist() {
        rideRepository.deleteAll();

        Iterable<Ride> rides = rideRepository.findAll();

        assertFalse(rides.iterator().hasNext(), "Should return an empty list when no rides exist");
    }

    @Test
    @DisplayName("Find all rides returns list bigger than 0")
    void findAllRidesTest() {
        Ride ride1 = createRideWithStops(origin, destination);
        Ride ride2 = createRideWithStops(origin, destination,
                FUTURE_DEPARTURE.plusSeconds(3600), null);

        rideRepository.save(ride1);
        rideRepository.save(ride2);

        Iterable<Ride> rides = rideRepository.findAll();

        assertTrue(rides.iterator().hasNext(), "Ride list should not be empty");
    }

    @Test
    @DisplayName("Attempt to update a non-existent ride returns exception")
    void updateNonExistentRide() {
        Ride nonExistentRide = Ride.builder()
                .id(NON_EXISTENT_ID)
                .departureTime(FUTURE_DEPARTURE)
                .description(RIDE_DESCRIPTION)
                .build();

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            rideRepository.save(nonExistentRide);
        }, "Updating non-existent ride should throw exception");
    }

    // ──────────────────────── Custom query methods ────────────────────────

    private Ride persistActiveRide() {
        Ride r = createRideWithStops(origin, destination);
        return rideRepository.saveAndFlush(r);
    }

    private Ride persistActiveRide(Instant departureTime, Instant estimatedArrival) {
        Ride r = createRideWithStops(origin, destination, departureTime, estimatedArrival);
        return rideRepository.saveAndFlush(r);
    }

    private RideBooking persistBooking(Ride ride, UserAccount passenger, BookingStatus status) {
        RideBooking b = RideBooking.builder()
                .ride(ride).passenger(passenger)
                .boardStop(ride.getStops().get(0))
                .alightStop(ride.getStops().get(ride.getStops().size() - 1))
                .status(status).seatCount(1).bookedAt(Instant.now())
                .resolvedAt(status == BookingStatus.CONFIRMED ? Instant.now() : null)
                .build();
        entityManager.persist(b);
        entityManager.flush();
        return b;
    }

    @Nested
    @DisplayName("Load ride with pessimistic write lock")
    class FindByIdForUpdateTests {

        @Test
        @DisplayName("Returns the ride attached to the persistence context when it exists")
        void returnsRideAttachedToPersistenceContext() {
            Ride ride = persistActiveRide();
            entityManager.clear();

            Optional<Ride> found = rideRepository.findByIdForUpdate(ride.getId());

            assertThat(found).isPresent()
                    .get().extracting(Ride::getId).isEqualTo(ride.getId());
        }

        @Test
        @DisplayName("Returns empty when the ride id does not exist")
        void returnsEmptyForMissingId() {
            assertThat(rideRepository.findByIdForUpdate(NON_EXISTENT_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("List active rides ready to be auto-completed")
    class FindActiveRidesReadyForCompletionTests {

        @Test
        @DisplayName("Includes active rides with confirmed bookings whose arrival has passed")
        void returnsActiveRidesWithConfirmedBookingsPastArrival() {
            Ride ride = persistActiveRide(
                    Instant.now().minus(2, ChronoUnit.HOURS),
                    Instant.now().minus(1, ChronoUnit.HOURS));
            UserAccount pax = userAccountRepository.save(
                    anActiveUserAccount().email("pax1@example.com").build());
            entityManager.flush();
            persistBooking(ride, pax, BookingStatus.CONFIRMED);
            entityManager.clear();

            List<Ride> ready = rideRepository.findActiveRidesReadyForCompletion(Instant.now());

            assertThat(ready).extracting(Ride::getId).contains(ride.getId());
        }

        @Test
        @DisplayName("Excludes rides that have no confirmed bookings")
        void excludesRidesWithoutConfirmedBooking() {
            Ride ride = persistActiveRide(
                    Instant.now().minus(2, ChronoUnit.HOURS),
                    Instant.now().minus(1, ChronoUnit.HOURS));
            entityManager.clear();

            List<Ride> ready = rideRepository.findActiveRidesReadyForCompletion(Instant.now());

            assertThat(ready).extracting(Ride::getId).doesNotContain(ride.getId());
        }

        @Test
        @DisplayName("Excludes rides that do not yet have an estimated arrival time")
        void excludesRidesWithNullEstimatedArrival() {
            Ride ride = persistActiveRide(Instant.now().minus(2, ChronoUnit.HOURS), null);
            UserAccount pax = userAccountRepository.save(
                    anActiveUserAccount().email("pax2@example.com").build());
            entityManager.flush();
            persistBooking(ride, pax, BookingStatus.CONFIRMED);
            entityManager.clear();

            List<Ride> ready = rideRepository.findActiveRidesReadyForCompletion(Instant.now());

            assertThat(ready).extracting(Ride::getId).doesNotContain(ride.getId());
        }
    }

    @Nested
    @DisplayName("List active rides ready to be expired (no bookings)")
    class FindActiveRidesReadyForExpiryTests {

        @Test
        @DisplayName("Includes active rides whose departure has passed and have no confirmed bookings")
        void returnsRidesWithNoConfirmedBookingsPastDeparture() {
            Ride ride = persistActiveRide(
                    Instant.now().minus(1, ChronoUnit.HOURS), null);
            entityManager.clear();

            List<Ride> expired = rideRepository.findActiveRidesWithNoBookingsReadyForExpiry(Instant.now());

            assertThat(expired).extracting(Ride::getId).contains(ride.getId());
        }

        @Test
        @DisplayName("Excludes rides that already have a confirmed booking")
        void excludesRidesWithConfirmedBooking() {
            Ride ride = persistActiveRide(
                    Instant.now().minus(1, ChronoUnit.HOURS), null);
            UserAccount pax = userAccountRepository.save(
                    anActiveUserAccount().email("pax3@example.com").build());
            entityManager.flush();
            persistBooking(ride, pax, BookingStatus.CONFIRMED);
            entityManager.clear();

            List<Ride> expired = rideRepository.findActiveRidesWithNoBookingsReadyForExpiry(Instant.now());

            assertThat(expired).extracting(Ride::getId).doesNotContain(ride.getId());
        }
    }

    @Nested
    @DisplayName("Load ride with stops and their locations eagerly")
    class FindByIdWithStopsAndLocationsTests {

        @Test
        @DisplayName("Returns the ride with stops and each stop's location initialised")
        void eagerLoadsStopsAndLocations() {
            Ride ride = persistActiveRide();
            entityManager.clear();

            Optional<Ride> found = rideRepository.findByIdWithStopsAndLocations(ride.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getStops()).hasSize(2)
                    .allSatisfy(s -> assertThat(s.getLocation()).isNotNull());
        }
    }

    @Nested
    @DisplayName("List distinct locations used by a driver's active future rides")
    class FindDistinctLocationsByDriverIdTests {

        @Test
        @DisplayName("Returns distinct locations used by the driver's active future rides")
        void returnsDistinctLocationsFromActiveFutureRides() {
            persistActiveRide(Instant.now().plus(2, ChronoUnit.HOURS), null);
            persistActiveRide(Instant.now().plus(3, ChronoUnit.HOURS), null);
            entityManager.clear();

            List<Location> locations = rideRepository.findDistinctLocationsByDriverId(
                    driver.getId(), Instant.now());

            assertThat(locations).hasSize(2)
                    .extracting(Location::getOsmId)
                    .containsExactlyInAnyOrder(OSM_ID_ORIGIN, OSM_ID_DESTINATION);
        }

        @Test
        @DisplayName("Excludes locations that only appear in rides already departed")
        void excludesLocationsFromPastRides() {
            persistActiveRide(Instant.now().minus(1, ChronoUnit.HOURS), null);
            entityManager.clear();

            List<Location> locations = rideRepository.findDistinctLocationsByDriverId(
                    driver.getId(), Instant.now());

            assertThat(locations).isEmpty();
        }
    }

    @Nested
    @DisplayName("List a driver's rides ordered by departure time")
    class FindByDriverIdTests {

        @Test
        @DisplayName("Returns the driver's rides ordered from earliest to latest departure")
        void returnsRidesInDepartureOrder() {
            Ride later = persistActiveRide(Instant.now().plus(2, ChronoUnit.HOURS), null);
            Ride earlier = persistActiveRide(Instant.now().plus(1, ChronoUnit.HOURS), null);
            entityManager.clear();

            List<Ride> rides = rideRepository.findByDriverIdOrderByDepartureTimeAscIdAsc(driver.getId());

            assertThat(rides).extracting(Ride::getId)
                    .containsExactly(earlier.getId(), later.getId());
        }
    }

    @Nested
    @DisplayName("Check driver ownership of a ride")
    class ExistsByIdAndDriverIdTests {

        @Test
        @DisplayName("Returns true when the ride is owned by the given driver")
        void returnsTrueForOwner() {
            Ride ride = persistActiveRide();
            entityManager.clear();

            assertThat(rideRepository.existsByIdAndDriverId(ride.getId(), driver.getId())).isTrue();
        }

        @Test
        @DisplayName("Returns false when the user is not the driver of the ride")
        void returnsFalseForNonOwner() {
            Ride ride = persistActiveRide();
            entityManager.clear();

            assertThat(rideRepository.existsByIdAndDriverId(ride.getId(), NON_EXISTENT_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("List rides by status completed in a time window")
    class FindByStatusAndCompletedAtBetweenTests {

        @Test
        @DisplayName("Returns rides with the given status completed within the time window")
        void returnsRidesWithinRange() {
            Ride ride = persistActiveRide();
            ride.markCompleted(Instant.now().minus(30, ChronoUnit.MINUTES));
            rideRepository.saveAndFlush(ride);
            entityManager.clear();

            List<Ride> result = rideRepository.findByStatusAndCompletedAtBetween(
                    Status.COMPLETED,
                    Instant.now().minus(1, ChronoUnit.HOURS),
                    Instant.now());

            assertThat(result).extracting(Ride::getId).contains(ride.getId());
        }

        @Test
        @DisplayName("Excludes rides completed outside the requested time window")
        void excludesRidesOutsideRange() {
            Ride ride = persistActiveRide();
            ride.markCompleted(Instant.now().minus(2, ChronoUnit.HOURS));
            rideRepository.saveAndFlush(ride);
            entityManager.clear();

            List<Ride> result = rideRepository.findByStatusAndCompletedAtBetween(
                    Status.COMPLETED,
                    Instant.now().minus(1, ChronoUnit.HOURS),
                    Instant.now());

            assertThat(result).extracting(Ride::getId).doesNotContain(ride.getId());
        }
    }
}

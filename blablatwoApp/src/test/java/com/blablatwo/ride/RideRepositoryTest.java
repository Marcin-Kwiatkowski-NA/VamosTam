package com.blablatwo.ride;

import com.blablatwo.domain.Status;
import com.blablatwo.location.Location;
import com.blablatwo.location.LocationRepository;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import com.blablatwo.user.UserProfileRepository;
import com.blablatwo.vehicle.Vehicle;
import com.blablatwo.vehicle.VehicleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static com.blablatwo.util.Constants.*;
import static com.blablatwo.util.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RideRepositoryTest {

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
        Ride ride = aRide(orig, dest)
                .id(null)
                .driver(driver)
                .vehicle(vehicle)
                .status(Status.ACTIVE)
                .stops(new ArrayList<>())
                .build();
        ride.getStops().addAll(buildStops(ride, orig, dest));
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
                () -> assertEquals(LOCAL_DATE, savedRide.getDepartureDate(), "Departure date should match"),
                () -> assertEquals(LOCAL_TIME, savedRide.getDepartureTime(), "Departure time should match"),
                () -> assertEquals(ONE, savedRide.getTotalSeats(), "Total seats should match"),
                () -> assertEquals(BIG_DECIMAL, savedRide.getPricePerSeat(), "Price per seat should match"),
                () -> assertEquals(vehicle.getId(), savedRide.getVehicle().getId(), "Vehicle should match"),
                () -> assertEquals(Status.ACTIVE, savedRide.getStatus(), "Status should match"),
                () -> assertEquals(INSTANT, savedRide.getLastModified(), "Last modified should match"),
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

        savedRide.setTotalSeats(2);
        savedRide.setPricePerSeat(BIG_DECIMAL.add(BigDecimal.ONE));
        savedRide.setLastModified(INSTANT.plusSeconds(60));
        savedRide.setStatus(Status.CANCELLED);
        savedRide.setDescription("Updated description");

        Ride updatedRide = rideRepository.save(savedRide);

        assertAll(
                () -> assertEquals(2, updatedRide.getTotalSeats(), "Total seats should be updated"),
                () -> assertEquals(BIG_DECIMAL.add(BigDecimal.ONE), updatedRide.getPricePerSeat(), "Price per seat should be updated"),
                () -> assertEquals(INSTANT.plusSeconds(60), updatedRide.getLastModified(), "Last modified should be updated"),
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
        Ride ride2 = createRideWithStops(origin, destination);
        ride2.setDepartureTime(LOCAL_TIME.plusHours(1));
        ride2.getStops().get(0).setDepartureTime(LOCAL_DATE.atTime(LOCAL_TIME.plusHours(1)));

        rideRepository.save(ride1);
        rideRepository.save(ride2);

        Iterable<Ride> rides = rideRepository.findAll();

        assertTrue(rides.iterator().hasNext(), "Ride list should not be empty");
    }

    @Test
    @DisplayName("Attempt to update a non-existent ride returns exception")
    void updateNonExistentRide() {
        Ride nonExistentRide = new Ride();
        nonExistentRide.setId(NON_EXISTENT_ID);
        nonExistentRide.setDepartureDate(LOCAL_DATE);
        nonExistentRide.setDepartureTime(LOCAL_TIME);
        nonExistentRide.setDescription(RIDE_DESCRIPTION);

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            rideRepository.save(nonExistentRide);
        }, "Updating non-existent ride should throw exception");
    }
}

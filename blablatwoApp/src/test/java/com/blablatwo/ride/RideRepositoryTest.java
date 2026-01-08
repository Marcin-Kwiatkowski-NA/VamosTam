package com.blablatwo.ride;

import com.blablatwo.city.City;
import com.blablatwo.city.CityRepository;
import com.blablatwo.traveler.Role;
import com.blablatwo.traveler.Traveler;
import com.blablatwo.traveler.TravelerRepository;
import com.blablatwo.vehicle.Vehicle;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.blablatwo.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RideRepositoryTest {


    @Autowired
    private EntityManager entityManager;

    @Autowired
    private RideRepository rideRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private TravelerRepository travelerRepository;

    private Traveler driver;
    private Vehicle vehicle;
    private City destination;
    private City origin;

    @BeforeEach
    void setUp() {
        origin = City.builder()
                .osmId(ID_ONE)
                .name(CITY_NAME_ORIGIN)
                .build();
        destination = City.builder()
                .osmId(2L)
                .name(CITY_NAME_DESTINATION)
                .build();
        cityRepository.save(origin);
        cityRepository.save(destination);

        vehicle = Vehicle.builder().model("911").make("Porsche").build();
        driver = Traveler.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .enabled(ENABLED)
                .role(Role.DRIVER)
                .email(EMAIL)
                .phoneNumber(TELEPHONE)
                .name(CRISTIANO)
                .vehicles(List.of(vehicle))
                .build();
        travelerRepository.save(driver);
    }

    @Test
    @DisplayName("Save a new ride successfully")
    void saveNewRide() {
        // Arrange
        Ride ride = Ride.builder()
                .driver(driver)
                .origin(origin)
                .destination(destination)
                .departureTime(LOCAL_DATE_TIME)
                .availableSeats(ONE)
                .pricePerSeat(BIG_DECIMAL)
                .vehicle(vehicle)
                .rideStatus(RideStatus.OPEN)
                .lastModified(INSTANT)
                .description(RIDE_DESCRIPTION)
                .build();

        // Act
        Ride savedRide = rideRepository.save(ride);

        // Assert
        assertAll(
                () -> assertNotNull(savedRide.getId(), "Saved ride should have an ID"),
                () -> assertEquals(driver.getId(), savedRide.getDriver().getId(), "Driver should match"),
                () -> assertEquals(origin.getId(), savedRide.getOrigin().getId(), "Origin should match"),
                () -> assertEquals(origin.getOsmId(), savedRide.getOrigin().getOsmId(), "Origin OSM ID should match"),
                () -> assertEquals(destination.getId(), savedRide.getDestination().getId(), "Destination should match"),
                () -> assertEquals(destination.getOsmId(), savedRide.getDestination().getOsmId(), "Destination OSM ID should match"),
                () -> assertEquals(LOCAL_DATE_TIME, savedRide.getDepartureTime(), "Departure time should match"),
                () -> assertEquals(ONE, savedRide.getAvailableSeats(), "Available seats should match"),
                () -> assertEquals(BIG_DECIMAL, savedRide.getPricePerSeat(), "Price per seat should match"),
                () -> assertEquals(vehicle.getId(), savedRide.getVehicle().getId(), "Vehicle should match"),
                () -> assertEquals(RideStatus.OPEN, savedRide.getRideStatus(), "Ride status should match"),
                () -> assertEquals(INSTANT, savedRide.getLastModified(), "Last modified should match"),
                () -> assertEquals(RIDE_DESCRIPTION, savedRide.getDescription(), "Description should match")
        );
    }

    @Test
    @DisplayName("Find a ride by valid ID")
    void findRideById() {
        // Arrange
        Ride ride = Ride.builder()
                .driver(driver)
                .origin(origin)
                .destination(destination)
                .departureTime(LOCAL_DATE_TIME)
                .availableSeats(ONE)
                .pricePerSeat(BIG_DECIMAL)
                .vehicle(vehicle)
                .rideStatus(RideStatus.OPEN)
                .lastModified(INSTANT)
                .description(RIDE_DESCRIPTION)
                .build();
        Ride savedRide = rideRepository.save(ride);

        // Act
        Optional<Ride> retrievedRide = rideRepository.findById(savedRide.getId());

        // Assert
        assertAll(
                () -> assertTrue(retrievedRide.isPresent(), "Ride should be found by ID"),
                () -> assertEquals(savedRide.getId(), retrievedRide.get().getId(), "IDs should match"),
                () -> assertEquals(RideStatus.OPEN, retrievedRide.get().getRideStatus(), "Ride status should match")
        );
    }

    @Test
    @DisplayName("Update a ride's details successfully")
    void shouldUpdateRideDetails() {
        // Arrange
        Ride ride = Ride.builder()
                .driver(driver)
                .origin(origin)
                .destination(destination)
                .departureTime(LOCAL_DATE_TIME)
                .availableSeats(ONE)
                .pricePerSeat(BIG_DECIMAL)
                .vehicle(vehicle)
                .rideStatus(RideStatus.OPEN)
                .lastModified(INSTANT)
                .description(RIDE_DESCRIPTION)
                .build();
        Ride savedRide = rideRepository.save(ride);

        savedRide.setAvailableSeats(2);
        savedRide.setPricePerSeat(BIG_DECIMAL.add(BigDecimal.ONE));
        savedRide.setLastModified(INSTANT.plusSeconds(60));
        savedRide.setRideStatus(RideStatus.COMPLETED);
        savedRide.setDescription("Updated description");

        // Act
        Ride updatedRide = rideRepository.save(savedRide);

        // Assert
        assertAll(
                () -> assertEquals(2, updatedRide.getAvailableSeats(), "Available seats should be updated"),
                () -> assertEquals(BIG_DECIMAL.add(BigDecimal.ONE), updatedRide.getPricePerSeat(), "Price per seat should be updated"),
                () -> assertEquals(INSTANT.plusSeconds(60), updatedRide.getLastModified(), "Last modified should be updated"),
                () -> assertEquals(RideStatus.COMPLETED, updatedRide.getRideStatus(), "Ride status should be updated"),
                () -> assertEquals("Updated description", updatedRide.getDescription(), "Description should be updated")
        );
    }

    @Test
    @DisplayName("Delete a ride successfully")
    void shouldDeleteRide() {
        // Arrange
        Ride ride = Ride.builder()
                .origin(origin)
                .destination(destination)
                .departureTime(LOCAL_DATE_TIME)
                .description(RIDE_DESCRIPTION)
                .build();
        Ride savedRide = rideRepository.save(ride);

        // Act
        rideRepository.deleteById(savedRide.getId());

        // Assert
        Optional<Ride> deletedRide = rideRepository.findById(savedRide.getId());
        assertFalse(deletedRide.isPresent(), "Ride should be deleted successfully");
    }

    @Test
    @DisplayName("Return empty when finding by non-existent ID")
    void returnEmptyForNonExistentId() {
        // Act
        Optional<Ride> retrievedRide = rideRepository.findById(NON_EXISTENT_ID);

        // Assert
        assertFalse(retrievedRide.isPresent(), "No ride should be found with non-existent ID");
    }

    @Test
    @DisplayName("Save a ride with null destination throws exception")
    void saveRideWithNullDestination() {
        // Arrange
        Ride ride = Ride.builder()
                .origin(origin)
                .departureTime(LOCAL_DATE_TIME)
                .description(RIDE_DESCRIPTION)
                .build();

        // Act & Assert
        assertThrows(ConstraintViolationException.class, () -> {
            rideRepository.save(ride);
            entityManager.flush();
        }, "Saving a ride with null destination should throw an exception");
    }

    @Test
    @DisplayName("Find all rides when no rides exist returns empty list")
    void findAllRidesWhenNoneExist() {
        // Arrange
        rideRepository.deleteAll();

        // Act
        Iterable<Ride> rides = rideRepository.findAll();


        // Assert
        assertFalse(rides.iterator().hasNext(), "Should return an empty list when no rides exist");
    }

    @Test
    @DisplayName("Find all rides returns list bigger than 0")
    void findAllRidesTest() {
        // Arrange
        Ride ride1 = Ride.builder()
                .origin(origin)
                .destination(destination)
                .departureTime(LOCAL_DATE_TIME)
                .description(RIDE_DESCRIPTION)
                .build();
        Ride ride2 = Ride.builder()
                .origin(origin)
                .destination(destination)
                .departureTime(LOCAL_DATE_TIME.plusHours(1))
                .description(RIDE_DESCRIPTION)
                .build();
        rideRepository.save(ride1);
        rideRepository.save(ride2);

        // Act
        Iterable<Ride> rides = rideRepository.findAll();

        // Assert
        assertTrue(rides.iterator().hasNext(), "Ride list should not be empty");
    }

    @SuppressWarnings("unchecked")
    @Test
    void printConstraint7() {
        var rows = entityManager.createNativeQuery("""
      SELECT CONSTRAINT_NAME, CHECK_CLAUSE
      FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS
      WHERE CONSTRAINT_NAME = 'CONSTRAINT_7'
      """).getResultList();

        rows.forEach(r -> System.out.println(java.util.Arrays.toString((Object[]) r)));
    }

    @Test
    @DisplayName("Attempt to update a non-existent ride returns exception")
    void updateNonExistentRide() {
        // Arrange
        Ride nonExistentRide = new Ride();
        nonExistentRide.setId(NON_EXISTENT_ID);
        nonExistentRide.setOrigin(origin);
        nonExistentRide.setDestination(destination);
        nonExistentRide.setDepartureTime(LOCAL_DATE_TIME);
        nonExistentRide.setDescription(RIDE_DESCRIPTION);

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            rideRepository.save(nonExistentRide);
        }, "Updating non-existent ride should throw exception");
    }
}

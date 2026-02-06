package com.blablatwo.ride;

import com.blablatwo.city.City;
import com.blablatwo.city.CityRepository;
import com.blablatwo.domain.Segment;
import com.blablatwo.domain.Status;
import com.blablatwo.domain.TimeSlot;
import com.blablatwo.user.AccountStatus;
import com.blablatwo.user.Role;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import com.blablatwo.user.UserProfile;
import com.blablatwo.user.UserProfileRepository;
import com.blablatwo.user.UserStats;
import com.blablatwo.vehicle.Vehicle;
import com.blablatwo.vehicle.VehicleRepository;
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
import java.util.Optional;
import java.util.Set;

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
    private UserAccountRepository userAccountRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private VehicleRepository vehicleRepository;

    private UserAccount driver;
    private Vehicle vehicle;
    private City destination;
    private City origin;

    @BeforeEach
    void setUp() {
        origin = City.builder()
                .placeId(ID_ONE)
                .namePl(CITY_NAME_ORIGIN)
                .normNamePl(CITY_NAME_ORIGIN.toLowerCase())
                .build();
        destination = City.builder()
                .placeId(2L)
                .namePl(CITY_NAME_DESTINATION)
                .normNamePl(CITY_NAME_DESTINATION.toLowerCase())
                .build();
        cityRepository.save(origin);
        cityRepository.save(destination);

        driver = UserAccount.builder()
                .email(EMAIL)
                .passwordHash(PASSWORD)
                .status(AccountStatus.ACTIVE)
                .roles(Set.of(Role.USER))
                .build();
        userAccountRepository.save(driver);

        userProfileRepository.save(UserProfile.builder()
                .account(driver)
                .displayName(CRISTIANO)
                .phoneNumber(TELEPHONE)
                .stats(new UserStats())
                .build());

        vehicle = Vehicle.builder().model("911").make("Porsche").owner(driver).build();
        vehicleRepository.save(vehicle);
    }

    @Test
    @DisplayName("Save a new ride successfully")
    void saveNewRide() {
        // Arrange
        Ride ride = Ride.builder()
                .driver(driver)
                .segment(new Segment(origin, destination))
                .timeSlot(new TimeSlot(LOCAL_DATE, LOCAL_TIME, false))
                .availableSeats(ONE)
                .pricePerSeat(BIG_DECIMAL)
                .vehicle(vehicle)
                .status(Status.ACTIVE)
                .lastModified(INSTANT)
                .description(RIDE_DESCRIPTION)
                .build();

        // Act
        Ride savedRide = rideRepository.save(ride);

        // Assert
        assertAll(
                () -> assertNotNull(savedRide.getId(), "Saved ride should have an ID"),
                () -> assertEquals(driver.getId(), savedRide.getDriver().getId(), "Driver should match"),
                () -> assertEquals(origin.getId(), savedRide.getSegment().getOrigin().getId(), "Origin should match"),
                () -> assertEquals(origin.getPlaceId(), savedRide.getSegment().getOrigin().getPlaceId(), "Origin placeId should match"),
                () -> assertEquals(destination.getId(), savedRide.getSegment().getDestination().getId(), "Destination should match"),
                () -> assertEquals(destination.getPlaceId(), savedRide.getSegment().getDestination().getPlaceId(), "Destination placeId should match"),
                () -> assertEquals(LOCAL_DATE, savedRide.getTimeSlot().getDepartureDate(), "Departure date should match"),
                () -> assertEquals(LOCAL_TIME, savedRide.getTimeSlot().getDepartureTime(), "Departure time should match"),
                () -> assertEquals(ONE, savedRide.getAvailableSeats(), "Available seats should match"),
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
        // Arrange
        Ride ride = Ride.builder()
                .driver(driver)
                .segment(new Segment(origin, destination))
                .timeSlot(new TimeSlot(LOCAL_DATE, LOCAL_TIME, false))
                .availableSeats(ONE)
                .pricePerSeat(BIG_DECIMAL)
                .vehicle(vehicle)
                .status(Status.ACTIVE)
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
                () -> assertEquals(Status.ACTIVE, retrievedRide.get().getStatus(), "Status should match")
        );
    }

    @Test
    @DisplayName("Update a ride's details successfully")
    void shouldUpdateRideDetails() {
        // Arrange
        Ride ride = Ride.builder()
                .driver(driver)
                .segment(new Segment(origin, destination))
                .timeSlot(new TimeSlot(LOCAL_DATE, LOCAL_TIME, false))
                .availableSeats(ONE)
                .pricePerSeat(BIG_DECIMAL)
                .vehicle(vehicle)
                .status(Status.ACTIVE)
                .lastModified(INSTANT)
                .description(RIDE_DESCRIPTION)
                .build();
        Ride savedRide = rideRepository.save(ride);

        savedRide.setAvailableSeats(2);
        savedRide.setPricePerSeat(BIG_DECIMAL.add(BigDecimal.ONE));
        savedRide.setLastModified(INSTANT.plusSeconds(60));
        savedRide.setStatus(Status.CANCELLED);
        savedRide.setDescription("Updated description");

        // Act
        Ride updatedRide = rideRepository.save(savedRide);

        // Assert
        assertAll(
                () -> assertEquals(2, updatedRide.getAvailableSeats(), "Available seats should be updated"),
                () -> assertEquals(BIG_DECIMAL.add(BigDecimal.ONE), updatedRide.getPricePerSeat(), "Price per seat should be updated"),
                () -> assertEquals(INSTANT.plusSeconds(60), updatedRide.getLastModified(), "Last modified should be updated"),
                () -> assertEquals(Status.CANCELLED, updatedRide.getStatus(), "Status should be updated"),
                () -> assertEquals("Updated description", updatedRide.getDescription(), "Description should be updated")
        );
    }

    @Test
    @DisplayName("Delete a ride successfully")
    void shouldDeleteRide() {
        // Arrange
        Ride ride = Ride.builder()
                .segment(new Segment(origin, destination))
                .timeSlot(new TimeSlot(LOCAL_DATE, LOCAL_TIME, false))
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
    @DisplayName("Save a ride with null segment throws exception")
    void saveRideWithNullSegment() {
        // Arrange
        Ride ride = Ride.builder()
                .timeSlot(new TimeSlot(LOCAL_DATE, LOCAL_TIME, false))
                .description(RIDE_DESCRIPTION)
                .build();

        // Act & Assert
        assertThrows(ConstraintViolationException.class, () -> {
            rideRepository.save(ride);
            entityManager.flush();
        }, "Saving a ride with null segment should throw an exception");
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
                .segment(new Segment(origin, destination))
                .timeSlot(new TimeSlot(LOCAL_DATE, LOCAL_TIME, false))
                .description(RIDE_DESCRIPTION)
                .build();
        Ride ride2 = Ride.builder()
                .segment(new Segment(origin, destination))
                .timeSlot(new TimeSlot(LOCAL_DATE, LOCAL_TIME.plusHours(1), false))
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
        nonExistentRide.setSegment(new Segment(origin, destination));
        nonExistentRide.setTimeSlot(new TimeSlot(LOCAL_DATE, LOCAL_TIME, false));
        nonExistentRide.setDescription(RIDE_DESCRIPTION);

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            rideRepository.save(nonExistentRide);
        }, "Updating non-existent ride should throw exception");
    }
}

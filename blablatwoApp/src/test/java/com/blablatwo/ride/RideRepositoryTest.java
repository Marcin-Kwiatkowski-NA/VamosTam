package com.blablatwo.ride;

import com.blablatwo.city.CityEntity;
import com.blablatwo.traveler.TravelerEntity;
import com.blablatwo.traveler.VehicleEntity;
import com.blablatwo.util.TestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static com.blablatwo.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@TestPropertySource("/application-test.properties")
@Import(TestConfig.class)
@DataJpaTest
class RideRepositoryTest {
  @Autowired
  RideRepository rideRepository;
  @Autowired
  public TravelerEntity driver;
  @Autowired
  private CityEntity origin;
  @Autowired
  private CityEntity destination;
  @Autowired
  private VehicleEntity vehicle;

  @Test
  @DisplayName("Find a ride by valid ID")
  @Order(1)
  void findRideById() {
    Optional<RideEntity> retrievedRide = rideRepository.findById(ID_100);

    assertTrue(retrievedRide.isPresent(), "Ride should be found by ID");
    assertEquals(ID_100, retrievedRide.get().getId(), "Ride ID should match");
  }

  @Test
  @DisplayName("Return empty when finding by non-existent ID")
  void returnEmptyForNonExistentId() {
    Optional<RideEntity> retrievedRide = rideRepository.findById(NON_EXISTENT_ID);

    assertFalse(retrievedRide.isPresent(), "No ride should be found with non-existent ID");
  }

  @Test
  @DisplayName("Save a new ride successfully")
  void saveNewRide() {
    // Arrange
    RideEntity ride = new RideEntity();
    ride.setDriver(driver);
    ride.setOrigin(origin);
    ride.setDestination(destination);
    ride.setDepartureTime(LOCAL_DATE_TIME);
    ride.setAvailableSeats(ONE);
    ride.setPricePerSeat(BIG_DECIMAL);
    ride.setVehicle(vehicle);
    ride.setRideStatus(RideStatus.OPEN);
    ride.setLastModified(INSTANT);

    // Act
    RideEntity savedRide = rideRepository.save(ride);

    // Assert
    assertNotNull(savedRide.getId(), "Saved ride should have an ID");
    assertEquals(driver.getId(), savedRide.getDriver().getId(), "Driver should match");
    assertEquals(origin.getId(), savedRide.getOrigin().getId(), "Origin should match");
    assertEquals(destination.getId(), savedRide.getDestination().getId(), "Destination should match");
  }

  @Test
  @DisplayName("Update a ride's details successfully")
  void shouldUpdateRideDetails() {
    // Arrange
    var newPrice = new BigDecimal("45.00");
    int newSeats = 2;
    Optional<RideEntity> rideOptional = rideRepository.findById(ID_100);
    assertTrue(rideOptional.isPresent(), "Ride should exist for update");
    RideEntity ride = rideOptional.get();

    // Modify ride details
    ride.setAvailableSeats(2);
    ride.setPricePerSeat(newPrice);
    ride.setRideStatus(RideStatus.FULL);

    // Act
    RideEntity updatedRide = rideRepository.save(ride);

    // Assert
    assertEquals(newSeats, updatedRide.getAvailableSeats(), "Available seats should be updated");
    assertEquals(newPrice, updatedRide.getPricePerSeat(), "Price per seat should be updated");
    assertEquals(RideStatus.FULL, updatedRide.getRideStatus(), "Ride status should be updated");
  }

  @Test
  @DisplayName("Delete a ride successfully")
  void shouldDeleteRide() {
    rideRepository.deleteById(ID_100);
    Optional<RideEntity> deletedRide = rideRepository.findById(ID_100);

    assertFalse(deletedRide.isPresent(), "Ride should be deleted successfully");
  }

  @Test
  @DisplayName("Attempt to update a non-existent ride throws exception")
  void updateNonExistentRide() {
    // Arrange
    RideEntity nonExistentRide = new RideEntity();
    nonExistentRide.setId(NON_EXISTENT_ID);

    // Act & Assert
    assertThrows(DataIntegrityViolationException.class, () -> {
      rideRepository.save(nonExistentRide);
    }, "Updating non-existent ride should throw an exception");
  }

  @Test
  @DisplayName("Save a ride with null driver throws exception")
  void saveRideWithNullDriver() {
    // Arrange
    RideEntity ride = new RideEntity();
    ride.setOrigin(origin);
    ride.setDestination(destination);
    ride.setDepartureTime(LOCAL_DATE_TIME);
    ride.setAvailableSeats(3);
    ride.setPricePerSeat(BIG_DECIMAL);
    ride.setVehicle(vehicle);
    ride.setRideStatus(RideStatus.OPEN);

    // Act & Assert
    assertThrows(DataIntegrityViolationException.class, () -> {
      rideRepository.save(ride);
    }, "Saving a ride with null driver should throw an exception");
  }

  @Test
  @DisplayName("Find all rides when no rides exist returns empty list")
  @Sql(scripts = "/datasets/clear_ride_data.sql")
  void findAllRidesWhenNoneExist() {
    // Act
    Iterable<RideEntity> rides = rideRepository.findAll();

    // Assert
    assertFalse(rides.iterator().hasNext(), "Should return an empty list when no rides exist");
  }
}

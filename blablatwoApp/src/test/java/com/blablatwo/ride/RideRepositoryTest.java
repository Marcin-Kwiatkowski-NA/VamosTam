package com.blablatwo.ride;

import com.blablatwo.RepositoryTest;
import com.blablatwo.city.City;
import com.blablatwo.traveler.Traveler;
import com.blablatwo.util.TestConfig;
import com.blablatwo.vehicle.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static com.blablatwo.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Import(TestConfig.class)
@DataJpaTest
class RideRepositoryTest extends RepositoryTest {
  @Autowired
  RideRepository rideRepository;

  @Mock
  Traveler driver;

  @Mock
  City origin;
  @Mock
  City destination;
  @Mock
  Vehicle vehicle;


  @Test
  @DisplayName("Find a ride by valid ID")
  @Order(1)
  void findRideById() {
    Optional<Ride> retrievedRide = rideRepository.findById(ID_100);

    assertTrue(retrievedRide.isPresent(), "Ride should be found by ID");
    assertEquals(ID_100, retrievedRide.get().getId(), "Ride ID should match");
  }

  @Test
  @DisplayName("Return empty when finding by non-existent ID")
  void returnEmptyForNonExistentId() {
    Optional<Ride> retrievedRide = rideRepository.findById(NON_EXISTENT_ID);

    assertFalse(retrievedRide.isPresent(), "No ride should be found with non-existent ID");
  }

  @Test
  @DisplayName("Save a new ride successfully")
  void saveNewRide() {
    // Arrange
    Ride ride = new Ride();
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
    Ride savedRide = rideRepository.save(ride);

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
    Optional<Ride> rideOptional = rideRepository.findById(ID_100);
    assertTrue(rideOptional.isPresent(), "Ride should exist for update");
    Ride ride = rideOptional.get();

    // Modify ride details
    ride.setAvailableSeats(2);
    ride.setPricePerSeat(newPrice);
    ride.setRideStatus(RideStatus.FULL);

    // Act
    Ride updatedRide = rideRepository.save(ride);

    // Assert
    assertEquals(newSeats, updatedRide.getAvailableSeats(), "Available seats should be updated");
    assertEquals(newPrice, updatedRide.getPricePerSeat(), "Price per seat should be updated");
    assertEquals(RideStatus.FULL, updatedRide.getRideStatus(), "Ride status should be updated");
  }

  @Test
  @DisplayName("Delete a ride successfully")
  void shouldDeleteRide() {
    rideRepository.deleteById(ID_100);
    Optional<Ride> deletedRide = rideRepository.findById(ID_100);

    assertFalse(deletedRide.isPresent(), "Ride should be deleted successfully");
  }

  @Test
  @DisplayName("Attempt to update a non-existent ride throws exception")
  void updateNonExistentRide() {
    // Arrange
    Ride nonExistentRide = new Ride();
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
    Ride ride = new Ride();
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
    Iterable<Ride> rides = rideRepository.findAll();

    // Assert
    assertFalse(rides.iterator().hasNext(), "Should return an empty list when no rides exist");
  }
}

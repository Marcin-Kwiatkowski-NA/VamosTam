package com.vamigo.vehicle;

import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.vamigo.AbstractIntegrationTest;

import java.util.List;
import java.util.Optional;

import static com.vamigo.util.Constants.*;
import static com.vamigo.util.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class VehicleRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    @DisplayName("Save a new vehicle successfully")
    void saveNewVehicle() {
        // Arrange
        Vehicle vehicle = aTesla().id(null).build();

        // Act
        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        // Assert
        assertAll(
                () -> assertNotNull(savedVehicle.getId(), "Saved vehicle should have an ID"),
                () -> assertEquals(VEHICLE_MAKE_TESLA, savedVehicle.getMake(), "Make should match"),
                () -> assertEquals(VEHICLE_MODEL_MODEL_S, savedVehicle.getModel(), "Model should match"),
                () -> assertEquals(VEHICLE_PRODUCTION_YEAR_2021, savedVehicle.getProductionYear(), "Production year should match"),
                () -> assertEquals(VEHICLE_COLOR_RED, savedVehicle.getColor(), "Color should match"),
                () -> assertEquals(VEHICLE_LICENSE_PLATE_1, savedVehicle.getLicensePlate(), "License plate should match")
        );
    }

    @Test
    @DisplayName("Find a vehicle by valid ID")
    void findVehicleById() {
        // Arrange
        Vehicle vehicle = aBmw().id(null).build();
        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        // Act
        Optional<Vehicle> retrievedVehicle = vehicleRepository.findById(savedVehicle.getId());

        // Assert
        assertAll(
                () -> assertTrue(retrievedVehicle.isPresent(), "Vehicle should be found by ID"),
                () -> assertEquals(VEHICLE_MAKE_BMW, retrievedVehicle.get().getMake(), "Make should match"),
                () -> assertEquals(VEHICLE_MODEL_X5, retrievedVehicle.get().getModel(), "Model should match"),
                () -> assertEquals(VEHICLE_PRODUCTION_YEAR_2020, retrievedVehicle.get().getProductionYear(), "Production year should match"),
                () -> assertEquals(VEHICLE_COLOR_BLACK, retrievedVehicle.get().getColor(), "Color should match"),
                () -> assertEquals(VEHICLE_LICENSE_PLATE_2, retrievedVehicle.get().getLicensePlate(), "License plate should match")
        );
    }

    @Test
    @DisplayName("Update a vehicle's details successfully")
    void shouldUpdateVehicleDetails() {
        // Arrange
        Vehicle vehicle = aTesla().id(null).build();
        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        // Update details
        savedVehicle.updateDetails(new VehicleDetails(
                savedVehicle.getMake(),
                savedVehicle.getModel(),
                savedVehicle.getProductionYear(),
                VEHICLE_COLOR_BLUE,
                VEHICLE_LICENSE_PLATE_3,
                savedVehicle.getDescription()
        ));

        // Act
        Vehicle updatedVehicle = vehicleRepository.save(savedVehicle);

        // Assert
        assertAll(
                () -> assertEquals(VEHICLE_COLOR_BLUE, updatedVehicle.getColor(), "Color should be updated"),
                () -> assertEquals(VEHICLE_LICENSE_PLATE_3, updatedVehicle.getLicensePlate(), "License plate should be updated")
        );
    }

    @Test
    @DisplayName("Delete a vehicle successfully")
    void shouldDeleteVehicle() {
        // Arrange
        Vehicle vehicle = Vehicle.builder()
                .make(VEHICLE_MAKE_TESLA)
                .model(VEHICLE_MODEL_MODEL_S)
                .build();
        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        // Act
        vehicleRepository.deleteById(savedVehicle.getId());

        // Assert
        Optional<Vehicle> deletedVehicle = vehicleRepository.findById(savedVehicle.getId());
        assertFalse(deletedVehicle.isPresent(), "Vehicle should be deleted successfully");
    }

    @Test
    @DisplayName("Return empty when finding by non-existent ID")
    void returnEmptyForNonExistentId() {
        // Act
        Optional<Vehicle> retrievedVehicle = vehicleRepository.findById(NON_EXISTENT_ID);

        // Assert
        assertFalse(retrievedVehicle.isPresent(), "No vehicle should be found with non-existent ID");
    }

    @Test
    @DisplayName("Save a vehicle with null make throws exception")
    void saveVehicleWithNullMake() {
        // Arrange
        Vehicle vehicle = Vehicle.builder()
                .model(VEHICLE_MODEL_MODEL_S)
                .build();

        // Act & Assert
        assertThrows(ConstraintViolationException.class, () -> {
            vehicleRepository.save(vehicle);
            entityManager.flush();
        }, "Saving a vehicle with null make should throw an exception");
    }

    @Test
    @DisplayName("Find all vehicles returns list bigger than 0")
    void findAllVehiclesTest() {
        // Arrange
        Vehicle vehicle1 = Vehicle.builder()
                .make(VEHICLE_MAKE_TESLA)
                .model(VEHICLE_MODEL_MODEL_S)
                .build();
        Vehicle vehicle2 = Vehicle.builder()
                .make(VEHICLE_MAKE_BMW)
                .model(VEHICLE_MODEL_X5)
                .build();
        vehicleRepository.save(vehicle1);
        vehicleRepository.save(vehicle2);

        // Act
        Iterable<Vehicle> vehicles = vehicleRepository.findAll();

        // Assert
        assertTrue(vehicles.iterator().hasNext(), "Vehicle list should not be empty");
    }

    @Nested
    @DisplayName("Lookup vehicle by license plate")
    class FindByLicensePlateTests {

        @Test
        @DisplayName("Returns vehicle when license plate matches a saved row")
        void returnsVehicleWhenPlateMatches() {
            Vehicle saved = vehicleRepository.save(aTesla().id(null).build());
            entityManager.flush();
            entityManager.clear();

            Optional<Vehicle> found = vehicleRepository.findByLicensePlate(VEHICLE_LICENSE_PLATE_1);

            assertThat(found).isPresent()
                    .get().extracting(Vehicle::getId).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("Returns empty when no vehicle has the given plate")
        void returnsEmptyWhenPlateMissing() {
            assertThat(vehicleRepository.findByLicensePlate("GHOST-1")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Lookup vehicles by owner id")
    class FindByOwnerIdTests {

        @Test
        @DisplayName("Returns only vehicles owned by the given user")
        void returnsVehiclesForOwner() {
            UserAccount owner = userAccountRepository.save(anActiveUserAccount().email("owner@example.com").build());
            UserAccount otherOwner = userAccountRepository.save(
                    anActiveUserAccount().email("other-owner@example.com").build());

            Vehicle owned1 = vehicleRepository.save(aTesla().id(null).owner(owner).build());
            Vehicle owned2 = vehicleRepository.save(aBmw().id(null).owner(owner)
                    .licensePlate(VEHICLE_LICENSE_PLATE_3).build());
            vehicleRepository.save(aBmw().id(null).owner(otherOwner)
                    .licensePlate("OTHER-1").build());
            entityManager.flush();
            entityManager.clear();

            List<Vehicle> result = vehicleRepository.findByOwnerId(owner.getId());

            assertThat(result).extracting(Vehicle::getId)
                    .containsExactlyInAnyOrder(owned1.getId(), owned2.getId());
        }

        @Test
        @DisplayName("Returns empty list when the owner has no vehicles")
        void returnsEmptyForOwnerWithNoVehicles() {
            UserAccount owner = userAccountRepository.save(
                    anActiveUserAccount().email("vacant@example.com").build());

            assertThat(vehicleRepository.findByOwnerId(owner.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Lookup vehicle by id scoped to owner")
    class FindByIdAndOwnerIdTests {

        @Test
        @DisplayName("Returns vehicle when id and owner both match")
        void returnsVehicleForCorrectOwner() {
            UserAccount owner = userAccountRepository.save(
                    anActiveUserAccount().email("scoped@example.com").build());
            Vehicle vehicle = vehicleRepository.save(aTesla().id(null).owner(owner).build());
            entityManager.flush();
            entityManager.clear();

            Optional<Vehicle> found = vehicleRepository.findByIdAndOwnerId(vehicle.getId(), owner.getId());

            assertThat(found).isPresent()
                    .get().extracting(Vehicle::getId).isEqualTo(vehicle.getId());
        }

        @Test
        @DisplayName("Returns empty when the vehicle is owned by a different user")
        void returnsEmptyWhenOwnerDiffers() {
            UserAccount owner = userAccountRepository.save(
                    anActiveUserAccount().email("owner2@example.com").build());
            UserAccount intruder = userAccountRepository.save(
                    anActiveUserAccount().email("intruder@example.com").build());
            Vehicle vehicle = vehicleRepository.save(aTesla().id(null).owner(owner).build());
            entityManager.flush();
            entityManager.clear();

            assertThat(vehicleRepository.findByIdAndOwnerId(vehicle.getId(), intruder.getId())).isEmpty();
        }
    }
}


package com.blablatwo.traveler.vehicle;

import com.blablatwo.vehicle.Vehicle;
import com.blablatwo.vehicle.VehicleRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static com.blablatwo.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class VehicleRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Test
    @DisplayName("Save a new vehicle successfully")
    void saveNewVehicle() {
        // Arrange
        Vehicle vehicle = Vehicle.builder()
                .make(VEHICLE_MAKE_TESLA)
                .model(VEHICLE_MODEL_MODEL_S)
                .productionYear(VEHICLE_PRODUCTION_YEAR_2021)
                .color(VEHICLE_COLOR_RED)
                .licensePlate(VEHICLE_LICENSE_PLATE_1)
                .build();

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
        Vehicle vehicle = Vehicle.builder()
                .make(VEHICLE_MAKE_BMW)
                .model(VEHICLE_MODEL_X5)
                .productionYear(VEHICLE_PRODUCTION_YEAR_2020)
                .color(VEHICLE_COLOR_BLACK)
                .licensePlate(VEHICLE_LICENSE_PLATE_2)
                .build();
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
        Vehicle vehicle = Vehicle.builder()
                .make(VEHICLE_MAKE_TESLA)
                .model(VEHICLE_MODEL_MODEL_S)
                .productionYear(VEHICLE_PRODUCTION_YEAR_2021)
                .color(VEHICLE_COLOR_RED)
                .licensePlate(VEHICLE_LICENSE_PLATE_1)
                .build();
        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        // Update details
        savedVehicle.setColor(VEHICLE_COLOR_BLUE);
        savedVehicle.setLicensePlate(VEHICLE_LICENSE_PLATE_3);

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
}


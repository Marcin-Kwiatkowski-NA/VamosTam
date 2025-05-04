package com.blablatwo.city;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;

import static com.blablatwo.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CityRepositoryImplTest  {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CityRepository cityRepository;

    @Test
    @DisplayName("Find a city by valid ID")
    void findCityById() {
        // Arrange
        City city = City.builder().name(CITY_NAME_KRAKOW).build();
        var savedCity = cityRepository.save(city);

        // Act
        Optional<City> retrievedCity = cityRepository.findById(savedCity.getId());

        // Assert
        assertAll(
                () -> assertTrue(retrievedCity.isPresent(), "City should be found by ID"),
                () -> assertEquals(CITY_NAME_KRAKOW, retrievedCity.get().getName(), "City name should match")
        );
    }

    @Test
    @DisplayName("Save a new city successfully")
    @Order(1)
    void saveNewCity() {
        // Arrange
        City city = City.builder().name(CITY_NAME_KRAKOW).build();

        // Act
        var savedCity = cityRepository.save(city);

        // Assert
        assertAll(
                () -> assertNotNull(savedCity.getId(), "Saved city should have an ID"),
                () -> assertEquals(CITY_NAME_KRAKOW, savedCity.getName(), "City name should match")
        );
    }

    @Test
    @DisplayName("Return empty when finding by non-existent ID")
    void returnEmptyForNonExistentId() {
        // Act
        Optional<City> retrievedCity = cityRepository.findById(NON_EXISTENT_ID);

        // Assert
        assertFalse(retrievedCity.isPresent(), "No city should be found with non-existent ID");
    }

    @Test
    @DisplayName("Update a city's details successfully")
    void shouldUpdateCityDetails() {
        // Arrange
        City city = City.builder().name(CITY_NAME_KRAKOW).build();

        // Act
        var saved = cityRepository.save(city);
        var savedId = saved.getId();
        saved.setName(CITY_NAME_DESTINATION);
        var updatedCity = cityRepository.save(city);

        // Assert
        assertAll(
                () -> assertEquals(savedId, updatedCity.getId(), "Updated city should have an ID"),
                () -> assertEquals(CITY_NAME_DESTINATION, updatedCity.getName(), "City name should be updated")
        );
    }

    @Test
    @DisplayName("Delete a city successfully")
    void shouldDeleteCity() {
        // Arrange
        cityRepository.deleteById(ID_100);

        // Act
        Optional<City> deletedCity = cityRepository.findById(ID_100);

        // Assert
        assertFalse(deletedCity.isPresent(), "City should be deleted successfully");
    }

    @Test
    @DisplayName("Attempt to update a non-existent city returns exception")
    void updateNonExistentCity() {
        // Arrange
        City nonExistentCity = new City();
        nonExistentCity.setId(NON_EXISTENT_ID);
        nonExistentCity.setName("Non-Existent City");

        // Act & Assert
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            cityRepository.save(nonExistentCity);
        }, "Updating non-existent city should throw exception");
    }

    @Test
    @DisplayName("Save a city with null name throws exception")
    void saveCityWithNullName() {
        // Arrange
        City city = new City();

        // Act & Assert
        assertThrows(ConstraintViolationException.class, () -> {
            cityRepository.save(city);
            entityManager.flush();
        }, "Saving a city with null name should throw an exception");
    }

    @Test
    @DisplayName("Find all cities when no cities exist returns empty list")
    void findAllCitiesWhenNoneExist() {
        // Arrange
        cityRepository.deleteAll();

        // Act
        Iterable<City> cities = cityRepository.findAll();

        // Assert
        assertFalse(cities.iterator().hasNext(), "Should return an empty list when no cities exist");
    }
}

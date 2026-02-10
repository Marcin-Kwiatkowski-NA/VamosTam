package com.blablatwo.city;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static com.blablatwo.util.Constants.*;
import static com.blablatwo.util.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class CityRepositoryImplTest {

    private static final String CITY_NAME_KRAKOW_NORMALIZED = "krakow";
    private static final String CITY_NAME_DESTINATION_NORMALIZED = "warsaw";

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CityRepository cityRepository;

    @Test
    @DisplayName("Find a city by valid ID")
    void findCityById() {
        // Arrange
        City city = aKrakowCity().id(null).build();
        var savedCity = cityRepository.save(city);

        // Act
        Optional<City> retrievedCity = cityRepository.findById(savedCity.getId());

        // Assert
        assertAll(
                () -> assertTrue(retrievedCity.isPresent(), "City should be found by ID"),
                () -> assertEquals(CITY_NAME_KRAKOW, retrievedCity.get().getNamePl(), "City name should match")
        );
    }

    @Test
    @DisplayName("Save a new city successfully")
    @Order(1)
    void saveNewCity() {
        // Arrange
        City city = aKrakowCity().id(null).build();

        // Act
        var savedCity = cityRepository.save(city);

        // Assert
        assertAll(
                () -> assertNotNull(savedCity.getId(), "Saved city should have an ID"),
                () -> assertEquals(CITY_NAME_KRAKOW, savedCity.getNamePl(), "City name should match"),
                () -> assertEquals(PLACE_ID_KRAKOW, savedCity.getPlaceId(), "PlaceId should match")
        );
    }

    @Test
    @DisplayName("Find a city by placeId")
    void findCityByPlaceId() {
        // Arrange
        City city = aKrakowCity().id(null).build();
        cityRepository.save(city);

        // Act
        Optional<City> retrievedCity = cityRepository.findByPlaceId(PLACE_ID_KRAKOW);

        // Assert
        assertAll(
                () -> assertTrue(retrievedCity.isPresent(), "City should be found by placeId"),
                () -> assertEquals(CITY_NAME_KRAKOW, retrievedCity.get().getNamePl(), "City name should match"),
                () -> assertEquals(PLACE_ID_KRAKOW, retrievedCity.get().getPlaceId(), "PlaceId should match")
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
    @DisplayName("Return empty when finding by non-existent placeId")
    void returnEmptyForNonExistentPlaceId() {
        // Act
        Optional<City> retrievedCity = cityRepository.findByPlaceId(999999L);

        // Assert
        assertFalse(retrievedCity.isPresent(), "No city should be found with non-existent placeId");
    }

    @Test
    @DisplayName("Update a city's details successfully")
    void shouldUpdateCityDetails() {
        // Arrange
        City city = aKrakowCity().id(null).build();

        // Act
        var saved = cityRepository.save(city);
        var savedId = saved.getId();
        saved.setNamePl(CITY_NAME_DESTINATION);
        saved.setNormNamePl(CITY_NAME_DESTINATION_NORMALIZED);
        saved.setPlaceId(PLACE_ID_WARSAW);
        var updatedCity = cityRepository.save(city);

        // Assert
        assertAll(
                () -> assertEquals(savedId, updatedCity.getId(), "Updated city should have same ID"),
                () -> assertEquals(CITY_NAME_DESTINATION, updatedCity.getNamePl(), "City name should be updated"),
                () -> assertEquals(PLACE_ID_WARSAW, updatedCity.getPlaceId(), "PlaceId should be updated")
        );
    }

    @Test
    @DisplayName("Delete a city successfully")
    void shouldDeleteCity() {
        // Arrange
        City city = aKrakowCity().id(null).build();
        City savedCity = cityRepository.save(city);
        Long savedId = savedCity.getId();

        // Act
        cityRepository.deleteById(savedId);

        // Assert
        Optional<City> deletedCity = cityRepository.findById(savedId);
        assertFalse(deletedCity.isPresent(), "City should be deleted successfully");
    }

    @Test
    @DisplayName("Attempt to update a non-existent city returns exception")
    void updateNonExistentCity() {
        // Arrange
        City nonExistentCity = new City();
        nonExistentCity.setId(NON_EXISTENT_ID);
        nonExistentCity.setPlaceId(PLACE_ID_KRAKOW);
        nonExistentCity.setNamePl("Non-Existent City");
        nonExistentCity.setNormNamePl("non-existent city");

        // Act & Assert
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            cityRepository.save(nonExistentCity);
        }, "Updating non-existent city should throw exception");
    }

    @Test
    @DisplayName("Save a city with null namePl throws exception")
    void saveCityWithNullNamePl() {
        // Arrange
        City city = City.builder()
                .placeId(PLACE_ID_KRAKOW)
                .normNamePl(CITY_NAME_KRAKOW_NORMALIZED)
                .build();

        // Act & Assert
        assertThrows(ConstraintViolationException.class, () -> {
            cityRepository.save(city);
            entityManager.flush();
        }, "Saving a city with null namePl should throw an exception");
    }

    @Test
    @DisplayName("Save a city with null placeId throws exception")
    void saveCityWithNullPlaceId() {
        // Arrange
        City city = City.builder()
                .namePl(CITY_NAME_KRAKOW)
                .normNamePl(CITY_NAME_KRAKOW_NORMALIZED)
                .build();

        // Act & Assert - DB constraint violation (Hibernate) rather than Bean Validation
        assertThrows(org.hibernate.exception.ConstraintViolationException.class, () -> {
            cityRepository.save(city);
            entityManager.flush();
        }, "Saving a city with null placeId should throw an exception");
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

    @Test
    @DisplayName("Find city by normalized Polish name")
    void findCityByNormNamePl() {
        // Arrange
        City city = aKrakowCity().id(null).build();
        cityRepository.save(city);

        // Act
        Optional<City> retrievedCity = cityRepository.findByNormNamePl(CITY_NAME_KRAKOW_NORMALIZED);

        // Assert
        assertAll(
                () -> assertTrue(retrievedCity.isPresent(), "City should be found by normalized name"),
                () -> assertEquals(CITY_NAME_KRAKOW, retrievedCity.get().getNamePl(), "City name should match")
        );
    }
}

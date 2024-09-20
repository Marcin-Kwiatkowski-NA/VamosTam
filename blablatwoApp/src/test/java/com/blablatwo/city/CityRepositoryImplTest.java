package com.blablatwo.city;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static com.blablatwo.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@TestPropertySource("/application-test.properties")
@JdbcTest
class CityRepositoryImplTest {

    CityRepository cityRepository;

    @Autowired
    public CityRepositoryImplTest(JdbcTemplate jdbcTemplate) {
        cityRepository = new CityRepositoryImpl(jdbcTemplate);
    }

    @Test
    @DisplayName("Find a city by valid ID")
    @Order(1)
    void findCityById() {
        Optional<CityEntity> retrievedCity = cityRepository.findById(ID_100_INT);

        assertTrue(retrievedCity.isPresent(), "City should be found by ID");
        assertEquals(CITY_NAME_KRAKOW, retrievedCity.get().getName(), "City name should match");
    }

    @Test
    @DisplayName("Save a new city successfully")
    void saveNewCity() {
        // Arrange
        String gdansk = "Gdańsk";
        CityEntity city = new CityEntity();
        city.setName(gdansk);

        // Act
        var savedCity = cityRepository.save(city);

        // Assert
        assertNotNull(savedCity.getId(), "Saved city should have an ID");
        assertEquals(gdansk, savedCity.getName(), "City name should match");
    }

    @Test
    @DisplayName("Return empty when finding by non-existent ID")
    void returnEmptyForNonExistentId() {
        Optional<CityEntity> retrievedCity = cityRepository.findById(NON_EXISTENT_ID_INT);

        assertFalse(retrievedCity.isPresent(), "No city should be found with non-existent ID");
    }

    @Test
    @DisplayName("Return size of test data set")
    void findAllCitiesTest() {
        var cities = cityRepository.findAll();

        assertEquals(TEST_CITY_TABLE_SIZE, cities.size());
    }

    @Test
    @DisplayName("Update a city's details successfully")
    void shouldUpdateCityDetails() {
        // Arrange
        CityEntity city = new CityEntity();
        String newName = "New Name";
        city.setName(newName);
        city.setId(ID_100_INT);

        // Act
        var updatedCity = cityRepository.update(city);

        // Assert
        assertTrue(updatedCity.isPresent(), "returns updated city");
        assertEquals(newName, updatedCity.get().getName(), "City name should be updated");
    }

    @Test
    @DisplayName("Delete a city successfully")
    void shouldDeleteCity() {
        boolean deletedSuccessfully = cityRepository.deleteById(ID_100_INT);
        Optional<CityEntity> deletedCity = cityRepository.findById(ID_100_INT);

        assertTrue(deletedSuccessfully, "Deleting should return true");
        assertFalse(deletedCity.isPresent(), "City should be deleted successfully");
    }

    @Test
    @DisplayName("Attempt to update a non-existent city returns empty Optional")
    void updateNonExistentCity() {
        // Arrange
        CityEntity nonExistentCity = new CityEntity();
        nonExistentCity.setId(NON_EXISTENT_ID_INT);
        nonExistentCity.setName("Non-Existent City");

        // Act
        Optional<CityEntity> updatedCity = cityRepository.update(nonExistentCity);

        // Assert
        assertFalse(updatedCity.isPresent(), "Updating non-existent city should return empty Optional");
    }

    @Test
    @DisplayName("Save a city with null name throws exception")
    void saveCityWithNullName() {
        // Arrange
        CityEntity city = new CityEntity();

        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () -> {
            cityRepository.save(city);
        }, "Saving a city with null name should throw an exception");
    }

    @Test
    @DisplayName("Find all cities when no cities exist returns empty list")
    @Sql(scripts = "/datasets/clear_city_data.sql")
    void findAllCitiesWhenNoneExist() {
        // Act
        List<CityEntity> cities = cityRepository.findAll();

        // Assert
        assertTrue(cities.isEmpty(), "Should return an empty list when no cities exist");
    }
}

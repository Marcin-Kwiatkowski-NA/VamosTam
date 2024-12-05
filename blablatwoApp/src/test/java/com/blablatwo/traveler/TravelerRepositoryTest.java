package com.blablatwo.traveler;

import com.blablatwo.RepositoryTest;
import com.blablatwo.config.Roles;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static com.blablatwo.config.Roles.ROLE_DRIVER;
import static com.blablatwo.traveler.TravelerType.DRIVER;
import static com.blablatwo.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class TravelerRepositoryTest extends RepositoryTest {

    @BeforeAll
    void setUp() {
        var driver = Traveler.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .enabled(ENABLED)
                .authority(ROLE_DRIVER)
                .email(EMAIL)
                .phoneNumber(TELEPHONE)
                .name(CRISTIANO)
                .type(DRIVER)
                .vehicles(List.of(vehicle))
                .build();
        travelerRepository.save(driver);
    }
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TravelerRepository travelerRepository;

    @MockitoBean
    VehicleEntity vehicle;

    @Test
    @DisplayName("Save a new traveler successfully")
    void saveNewTraveler() {
        // Arrange
        Traveler traveler = Traveler.builder()
                .username(TRAVELER_USERNAME_JOHN_DOE)
                .password(TRAVELER_PASSWORD)
                .enabled(ENABLED)
                .authority(Roles.ROLE_PASSENGER)
                .email(TRAVELER_EMAIL_JOHN_DOE)
                .phoneNumber(TRAVELER_PHONE_NUMBER)
                .name(TRAVELER_NAME_JOHN_DOE)
                .type(TravelerType.PASSENGER)
                .build();

        // Act
        Traveler savedTraveler = travelerRepository.save(traveler);

        // Assert
        assertAll(
                () -> assertNotNull(savedTraveler.getId(), "Saved traveler should have an ID"),
                () -> assertEquals(TRAVELER_USERNAME_JOHN_DOE, savedTraveler.getUsername(), "Username should match"),
                () -> assertEquals(TRAVELER_PASSWORD, savedTraveler.getPassword(), "Password should match"),
                () -> assertEquals(ENABLED, savedTraveler.getEnabled(), "Enabled status should match"),
                () -> assertEquals(Roles.ROLE_PASSENGER, savedTraveler.getAuthority(), "Authority should match"),
                () -> assertEquals(TRAVELER_EMAIL_JOHN_DOE, savedTraveler.getEmail(), "Email should match"),
                () -> assertEquals(TRAVELER_PHONE_NUMBER, savedTraveler.getPhoneNumber(), "Phone number should match"),
                () -> assertEquals(TRAVELER_NAME_JOHN_DOE, savedTraveler.getName(), "Name should match"),
                () -> assertEquals(TravelerType.PASSENGER, savedTraveler.getType(), "Traveler type should match")
        );
    }

    @Test
    @DisplayName("Find a traveler by valid ID")
    void findTravelerById() {
        // Arrange
        Traveler traveler = Traveler.builder()
                .username(TRAVELER_USERNAME_JANE_DOE)
                .password(TRAVELER_PASSWORD_SECURE)
                .enabled(ENABLED)
                .authority(Roles.ROLE_DRIVER)
                .email(TRAVELER_EMAIL_JANE_DOE)
                .phoneNumber(TRAVELER_PHONE_NUMBER)
                .name(TRAVELER_NAME_JANE_DOE)
                .type(TravelerType.DRIVER)
                .build();
        Traveler savedTraveler = travelerRepository.save(traveler);

        // Act
        Optional<Traveler> retrievedTraveler = travelerRepository.findById(savedTraveler.getId());

        // Assert
        assertAll(
                () -> assertTrue(retrievedTraveler.isPresent(), "Traveler should be found by ID"),
                () -> assertEquals(TRAVELER_USERNAME_JANE_DOE, retrievedTraveler.get().getUsername(), "Username should match"),
                () -> assertEquals(TRAVELER_PASSWORD_SECURE, retrievedTraveler.get().getPassword(), "Password should match"),
                () -> assertEquals(TRAVELER_NAME_JANE_DOE, retrievedTraveler.get().getName(), "Name should match"),
                () -> assertEquals(TRAVELER_EMAIL_JANE_DOE, retrievedTraveler.get().getEmail(), "Email should match")
        );
    }

    @Test
    @DisplayName("Update a traveler's details successfully")
    void shouldUpdateTravelerDetails() {
        // Arrange
        Traveler traveler = Traveler.builder()
                .username(TRAVELER_USERNAME_JANE_DOE)
                .password(TRAVELER_PASSWORD_SECURE)
                .enabled(ENABLED)
                .authority(Roles.ROLE_DRIVER)
                .email(TRAVELER_EMAIL_JANE_DOE)
                .phoneNumber(TRAVELER_PHONE_NUMBER)
                .name(TRAVELER_NAME_JANE_DOE)
                .type(TravelerType.DRIVER)
                .build();
        Traveler savedTraveler = travelerRepository.save(traveler);

        // Update details
        var travelerId = savedTraveler.getId();
        savedTraveler.setName(TRAVELER_NAME_JANE_SMITH);
        savedTraveler.setEmail(TRAVELER_NEW_EMAIL_JANE_DOE);
        travelerRepository.save(savedTraveler);

        // Act
        var updatedTraveler = travelerRepository.findById(travelerId);

        // Assert
        assertAll(
                () -> assertTrue(updatedTraveler.isPresent()),
                () -> assertEquals(TRAVELER_NAME_JANE_SMITH, updatedTraveler.get().getName(), "Name should be updated"),
                () -> assertEquals(TRAVELER_NEW_EMAIL_JANE_DOE, updatedTraveler.get().getEmail(), "Email should be updated")
        );
    }

    @Test
    @DisplayName("Delete a traveler successfully")
    void shouldDeleteTraveler() {
        // Arrange
        Traveler traveler = Traveler.builder()
                .username(TRAVELER_USERNAME_TO_DELETE)
                .password(TRAVELER_PASSWORD)
                .email(TRAVELER_EMAIL_TO_DELETE)
                .build();
        Traveler savedTraveler = travelerRepository.save(traveler);

        // Act
        travelerRepository.deleteById(savedTraveler.getId());

        // Assert
        Optional<Traveler> deletedTraveler = travelerRepository.findById(savedTraveler.getId());
        assertFalse(deletedTraveler.isPresent(), "Traveler should be deleted successfully");
    }

    @Test
    @DisplayName("Return empty when finding by non-existent ID")
    void returnEmptyForNonExistentId() {
        // Act
        Optional<Traveler> retrievedTraveler = travelerRepository.findById(NON_EXISTENT_ID);

        // Assert
        assertFalse(retrievedTraveler.isPresent(), "No traveler should be found with non-existent ID");
    }

    @Test
    @DisplayName("Attempt to update a non-existent traveler throws exception")
    void updateNonExistentTraveler() {
        // Arrange
        Traveler nonExistentTraveler = Traveler.builder()
                .id(NON_EXISTENT_ID)
                .username(TRAVELER_USERNAME_NON_EXISTENT)
                .password(TRAVELER_PASSWORD_NON_EXISTENT)
                .build();

        // Act & Assert
        assertThrows(OptimisticLockingFailureException.class, () -> {
            travelerRepository.save(nonExistentTraveler);
            entityManager.flush();
        }, "Updating non-existent traveler should throw exception");
    }

    @Test
    @DisplayName("Save a traveler with null username throws exception")
    void saveTravelerWithNullUsername() {
        // Arrange
        Traveler traveler = Traveler.builder()
                .password(TRAVELER_PASSWORD)
                .email(EMAIL)
                .build();

        // Act & Assert
        assertThrows(ConstraintViolationException.class, () -> {
            travelerRepository.save(traveler);
            entityManager.flush();
        }, "Saving a traveler with null username should throw an exception");
    }

    @Test
    @DisplayName("Find all travelers when no travelers exist returns empty list")
    void findAllTravelersWhenNoneExist() {
        // Arrange
        travelerRepository.deleteAll();

        // Act
        Iterable<Traveler> travelers = travelerRepository.findAll();

        // Assert
        assertFalse(travelers.iterator().hasNext(), "Should return an empty list when no travelers exist");
    }

    @Test
    @DisplayName("Find all travelers returns list bigger than 1")
    void findAllTravelersTest() {
        // Arrange
        Traveler traveler = Traveler.builder()
                .username(TRAVELER_USERNAME_JOHN_DOE)
                .password(TRAVELER_PASSWORD)
                .enabled(ENABLED)
                .authority(Roles.ROLE_PASSENGER)
                .email(TRAVELER_EMAIL_JOHN_DOE)
                .phoneNumber(TRAVELER_PHONE_NUMBER)
                .name(TRAVELER_NAME_JOHN_DOE)
                .type(TravelerType.PASSENGER)
                .build();
        travelerRepository.save(traveler);

        // Act
        List<Traveler> travelers = travelerRepository.findAll();
        System.out.println(travelers.size());

        // Assert
        assertTrue(travelers.size() > 1, "Traveler list should not be empty");
    }
}


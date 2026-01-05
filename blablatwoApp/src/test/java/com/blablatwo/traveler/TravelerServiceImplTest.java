package com.blablatwo.traveler;

import com.blablatwo.config.Roles;
import jakarta.persistence.EntityExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelerServiceImplTest {

    private static final Long ID_100 = 100L;
    private static final Long NON_EXISTENT_ID = 999L;
    private static final String USERNAME = "testuser";
    private static final String NON_EXISTENT_USERNAME = "nonexistent";
    private static final String PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encodedPassword123";
    private static final String EMAIL = "test@example.com";

    @Mock
    private TravelerRepository travelerRepository;

    @Mock
    private TravelerMapper travelerMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TravelerServiceImpl travelerService;

    private Traveler traveler;
    private TravelerCreationDto travelerCreationDto;
    private TravelerResponseDto travelerResponseDto;

    @BeforeEach
    void setUp() {
        traveler = Traveler.builder()
                .id(ID_100)
                .username(USERNAME)
                .password(ENCODED_PASSWORD)
                .email(EMAIL)
                .enabled(1)
                .authority(Roles.ROLE_PASSENGER)
                .type(TravelerType.PASSENGER)
                .build();

        travelerCreationDto = new TravelerCreationDto(
                USERNAME, PASSWORD, EMAIL, "123-456-7890", "Test User",
                Roles.ROLE_PASSENGER, TravelerType.PASSENGER
        );

        travelerResponseDto = new TravelerResponseDto(
                ID_100, USERNAME, EMAIL, "123-456-7890", "Test User",
                Roles.ROLE_PASSENGER, TravelerType.PASSENGER, null, Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Load user by username successfully")
    void loadUserByUsernameSuccessfully() {
        when(travelerRepository.findByUsername(USERNAME)).thenReturn(Optional.of(traveler));

        UserDetails userDetails = travelerService.loadUserByUsername(USERNAME);

        assertNotNull(userDetails);
        assertEquals(USERNAME, userDetails.getUsername());
        assertEquals(ENCODED_PASSWORD, userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority(Roles.ROLE_PASSENGER.name())));
        verify(travelerRepository).findByUsername(USERNAME);
    }

    @Test
    @DisplayName("Throw UsernameNotFoundException when loading non-existent username")
    void throwExceptionWhenLoadingNonExistentUsername() {
        when(travelerRepository.findByUsername(NON_EXISTENT_USERNAME)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> travelerService.loadUserByUsername(NON_EXISTENT_USERNAME));
        verify(travelerRepository).findByUsername(NON_EXISTENT_USERNAME);
    }

    @Test
    @DisplayName("Return traveler by existing ID")
    void getByIdReturnsTraveler() {
        when(travelerRepository.findById(ID_100)).thenReturn(Optional.of(traveler));

        Optional<Traveler> result = travelerService.getById(ID_100);

        assertTrue(result.isPresent());
        assertEquals(traveler, result.get());
        verify(travelerRepository).findById(ID_100);
        verify(travelerMapper, never()).travelerEntityToTravelerResponseDto(any());
    }

    @Test
    @DisplayName("Return empty when getting traveler by non-existent ID")
    void getByIdReturnsEmptyForNonExistentId() {
        when(travelerRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        Optional<Traveler> result = travelerService.getById(NON_EXISTENT_ID);

        assertFalse(result.isPresent());
        verify(travelerRepository).findById(NON_EXISTENT_ID);
        verify(travelerMapper, never()).travelerEntityToTravelerResponseDto(any());
    }

    @Test
    @DisplayName("Return traveler by existing username")
    void getByUsernameReturnsTraveler() {
        when(travelerRepository.findByUsername(USERNAME)).thenReturn(Optional.of(traveler));

        Optional<Traveler> result = travelerService.getByUsername(USERNAME);

        assertTrue(result.isPresent());
        assertEquals(traveler, result.get());
        verify(travelerRepository).findByUsername(USERNAME);
        verify(travelerMapper, never()).travelerEntityToTravelerResponseDto(any());
    }

    @Test
    @DisplayName("Return empty when getting traveler by non-existent username")
    void getByUsernameReturnsEmptyForNonExistentUsername() {
        when(travelerRepository.findByUsername(NON_EXISTENT_USERNAME)).thenReturn(Optional.empty());

        Optional<Traveler> result = travelerService.getByUsername(NON_EXISTENT_USERNAME);

        assertFalse(result.isPresent());
        verify(travelerRepository).findByUsername(NON_EXISTENT_USERNAME);
        verify(travelerMapper, never()).travelerEntityToTravelerResponseDto(any());
    }

    @Test
    @DisplayName("Return all travelers successfully")
    void getAllTravelersSuccessfully() {
        when(travelerRepository.findAll()).thenReturn(List.of(traveler));

        List<Traveler> result = travelerService.getAllTravelers();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(traveler, result.get(0));
        verify(travelerRepository).findAll();
        verify(travelerMapper, never()).travelerEntityToTravelerResponseDto(any());
    }

    @Test
    @DisplayName("Create new traveler successfully")
    void createNewTravelerSuccessfully() {
        when(travelerRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(travelerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(travelerMapper.travelerCreationDtoToEntity(travelerCreationDto)).thenReturn(traveler);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(travelerRepository.save(traveler)).thenReturn(traveler);
        when(travelerMapper.travelerEntityToTravelerResponseDto(traveler)).thenReturn(travelerResponseDto);

        TravelerResponseDto result = travelerService.create(travelerCreationDto);

        assertNotNull(result);
        assertEquals(travelerResponseDto, result);
        verify(travelerRepository).findByUsername(USERNAME);
        verify(travelerRepository).findByEmail(EMAIL);
        verify(travelerMapper).travelerCreationDtoToEntity(travelerCreationDto);
        verify(passwordEncoder).encode(PASSWORD);
        verify(travelerRepository).save(traveler);
        verify(travelerMapper).travelerEntityToTravelerResponseDto(traveler);
    }

    @Test
    @DisplayName("Throw EntityExistsException when creating traveler with existing username")
    void createThrowsExceptionForExistingUsername() {
        when(travelerRepository.findByUsername(USERNAME)).thenReturn(Optional.of(traveler));

        assertThrows(EntityExistsException.class,
                () -> travelerService.create(travelerCreationDto));
        verify(travelerRepository).findByUsername(USERNAME);
        verify(travelerRepository, never()).findByEmail(anyString());
        verify(travelerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Throw EntityExistsException when creating traveler with existing email")
    void createThrowsExceptionForExistingEmail() {
        when(travelerRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        when(travelerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(traveler));

        assertThrows(EntityExistsException.class,
                () -> travelerService.create(travelerCreationDto));
        verify(travelerRepository).findByUsername(USERNAME);
        verify(travelerRepository).findByEmail(EMAIL);
        verify(travelerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update existing traveler successfully")
    void updateExistingTravelerSuccessfully() {
        when(travelerRepository.findById(ID_100)).thenReturn(Optional.of(traveler));
        doNothing().when(travelerMapper).update(traveler, travelerCreationDto);
        when(travelerRepository.save(traveler)).thenReturn(traveler);
        when(travelerMapper.travelerEntityToTravelerResponseDto(traveler)).thenReturn(travelerResponseDto);

        TravelerResponseDto result = travelerService.update(travelerCreationDto, ID_100);

        assertNotNull(result);
        assertEquals(travelerResponseDto, result);
        verify(travelerRepository).findById(ID_100);
        verify(travelerMapper).update(traveler, travelerCreationDto);
        verify(travelerRepository).save(traveler);
        verify(travelerMapper).travelerEntityToTravelerResponseDto(traveler);
    }

    @Test
    @DisplayName("Throw UsernameNotFoundException when updating non-existent traveler")
    void throwExceptionWhenUpdatingNonExistentTraveler() {
        when(travelerRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> travelerService.update(travelerCreationDto, NON_EXISTENT_ID));
        verify(travelerRepository).findById(NON_EXISTENT_ID);
        verify(travelerMapper, never()).update(any(), any());
        verify(travelerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete existing traveler successfully")
    void deleteExistingTravelerSuccessfully() {
        when(travelerRepository.existsById(ID_100)).thenReturn(true);
        doNothing().when(travelerRepository).deleteById(ID_100);

        travelerService.delete(ID_100);

        verify(travelerRepository).existsById(ID_100);
        verify(travelerRepository).deleteById(ID_100);
    }

    @Test
    @DisplayName("Throw UsernameNotFoundException when deleting non-existent traveler")
    void throwExceptionWhenDeletingNonExistentTraveler() {
        when(travelerRepository.existsById(NON_EXISTENT_ID)).thenReturn(false);

        assertThrows(UsernameNotFoundException.class,
                () -> travelerService.delete(NON_EXISTENT_ID));
        verify(travelerRepository).existsById(NON_EXISTENT_ID);
        verify(travelerRepository, never()).deleteById(anyLong());
    }
}
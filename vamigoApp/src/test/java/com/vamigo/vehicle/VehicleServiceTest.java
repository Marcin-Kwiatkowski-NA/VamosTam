package com.vamigo.vehicle;

import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import com.vamigo.user.exception.NoSuchUserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.vamigo.util.Constants.*;
import static com.vamigo.util.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleMapper vehicleMapper;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private VehiclePhotoUrlResolver photoUrlResolver;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    private Vehicle vehicle;
    private VehicleCreationDto vehicleCreationDto;
    private VehicleResponseDto vehicleResponseDto;
    private UserAccount owner;

    @BeforeEach
    void setUp() {
        vehicle = aTesla().build();
        vehicleCreationDto = aVehicleCreation().build();
        vehicleResponseDto = aTeslaVehicleResponse().build();
        owner = aDriverAccount().id(ID_ONE).build();
    }

    @Test
    @DisplayName("Return vehicles for a given user")
    void returnVehiclesForUser() {
        when(vehicleRepository.findByOwnerId(ID_ONE)).thenReturn(List.of(vehicle));
        when(vehicleMapper.vehicleEntityToVehicleResponseDto(vehicle)).thenReturn(vehicleResponseDto);
        when(photoUrlResolver.resolve(vehicle)).thenReturn(null);

        List<VehicleResponseDto> result = vehicleService.getMyVehicles(ID_ONE);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(vehicleRepository).findByOwnerId(ID_ONE);
    }

    @Test
    @DisplayName("Create a new vehicle successfully")
    void createNewVehicleSuccessfully() {
        when(userAccountRepository.findById(ID_ONE)).thenReturn(Optional.of(owner));
        when(vehicleMapper.vehicleCreationDtoToEntity(vehicleCreationDto)).thenReturn(vehicle);
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
        when(vehicleMapper.vehicleEntityToVehicleResponseDto(vehicle)).thenReturn(vehicleResponseDto);
        when(photoUrlResolver.resolve(vehicle)).thenReturn(null);

        VehicleResponseDto result = vehicleService.create(ID_ONE, vehicleCreationDto);

        assertNotNull(result);
        verify(userAccountRepository).findById(ID_ONE);
        verify(vehicleMapper).vehicleCreationDtoToEntity(vehicleCreationDto);
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    @DisplayName("Throw NoSuchUserException when creating vehicle for non-existent user")
    void throwWhenCreatingForNonExistentUser() {
        when(userAccountRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        assertThrows(NoSuchUserException.class,
                () -> vehicleService.create(NON_EXISTENT_ID, vehicleCreationDto));
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update an existing vehicle successfully")
    void updateExistingVehicleSuccessfully() {
        when(vehicleRepository.findByIdAndOwnerId(ID_ONE, ID_ONE)).thenReturn(Optional.of(vehicle));
        doNothing().when(vehicleMapper).update(vehicle, vehicleCreationDto);
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
        when(vehicleMapper.vehicleEntityToVehicleResponseDto(vehicle)).thenReturn(vehicleResponseDto);
        when(photoUrlResolver.resolve(vehicle)).thenReturn(null);

        VehicleResponseDto result = vehicleService.update(ID_ONE, ID_ONE, vehicleCreationDto);

        assertNotNull(result);
        verify(vehicleRepository).findByIdAndOwnerId(ID_ONE, ID_ONE);
        verify(vehicleMapper).update(vehicle, vehicleCreationDto);
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    @DisplayName("Throw NoSuchElementException when updating non-existent vehicle")
    void throwWhenUpdatingNonExistentVehicle() {
        when(vehicleRepository.findByIdAndOwnerId(NON_EXISTENT_ID, ID_ONE)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> vehicleService.update(ID_ONE, NON_EXISTENT_ID, vehicleCreationDto));
        verify(vehicleMapper, never()).update(any(), any());
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete an existing vehicle successfully")
    void deleteExistingVehicleSuccessfully() {
        when(vehicleRepository.findByIdAndOwnerId(ID_ONE, ID_ONE)).thenReturn(Optional.of(vehicle));

        vehicleService.delete(ID_ONE, ID_ONE);

        verify(vehicleRepository).findByIdAndOwnerId(ID_ONE, ID_ONE);
        verify(vehicleRepository).delete(vehicle);
    }

    @Test
    @DisplayName("Throw NoSuchElementException when deleting non-existent vehicle")
    void throwWhenDeletingNonExistentVehicle() {
        when(vehicleRepository.findByIdAndOwnerId(NON_EXISTENT_ID, ID_ONE)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> vehicleService.delete(ID_ONE, NON_EXISTENT_ID));
        verify(vehicleRepository, never()).delete(any());
        verify(vehicleRepository, never()).deleteById(anyLong());
    }
}

package com.blablatwo.vehicle;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    private static final Long ID_100 = 100L;
    private static final Long NON_EXISTENT_ID = 999L;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleMapper vehicleMapper;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    private Vehicle vehicle;
    private VehicleCreationDto vehicleCreationDto;
    private VehicleResponseDto vehicleResponseDto;

    @BeforeEach
    void setUp() {
        vehicle = Vehicle.builder()
                .id(ID_100)
                .make("Toyota")
                .model("Camry")
                .productionYear(2020)
                .color("Blue")
                .licensePlate("KR12345")
                .build();

        vehicleCreationDto = new VehicleCreationDto(
                "Toyota", "Camry", 2020, "Blue", "KR12345"
        );

        vehicleResponseDto = new VehicleResponseDto(
                ID_100, "Toyota", "Camry", 2020, "Blue", "KR12345"
        );
    }

    @Test
    @DisplayName("Return vehicle when finding by existing ID")
    void returnVehicleWhenFindingByExistingId() {
        when(vehicleRepository.findById(ID_100)).thenReturn(Optional.of(vehicle));

        Optional<Vehicle> result = vehicleService.getById(ID_100);

        assertTrue(result.isPresent());
        assertEquals(vehicle, result.get());
        verify(vehicleRepository).findById(ID_100);
        verify(vehicleMapper, never()).vehicleEntityToVehicleResponseDto(any());
    }

    @Test
    @DisplayName("Return empty when finding by non-existent ID")
    void returnEmptyWhenFindingByNonExistentId() {
        when(vehicleRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        Optional<Vehicle> result = vehicleService.getById(NON_EXISTENT_ID);

        assertFalse(result.isPresent());
        verify(vehicleRepository).findById(NON_EXISTENT_ID);
        verify(vehicleMapper, never()).vehicleEntityToVehicleResponseDto(any());
    }

    @Test
    @DisplayName("Return all vehicles successfully")
    void returnAllVehiclesSuccessfully() {
        when(vehicleRepository.findAll()).thenReturn(List.of(vehicle));

        List<Vehicle> result = vehicleService.getAllVehicles();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(vehicle, result.get(0));
        verify(vehicleRepository).findAll();
        verify(vehicleMapper, never()).vehicleEntityToVehicleResponseDto(any());
    }

    @Test
    @DisplayName("Create a new vehicle successfully")
    void createNewVehicleSuccessfully() {
        when(vehicleMapper.vehicleCreationDtoToEntity(vehicleCreationDto)).thenReturn(vehicle);
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
        when(vehicleMapper.vehicleEntityToVehicleResponseDto(vehicle)).thenReturn(vehicleResponseDto);

        VehicleResponseDto result = vehicleService.create(vehicleCreationDto);

        assertNotNull(result);
        assertEquals(vehicleResponseDto, result);
        verify(vehicleMapper).vehicleCreationDtoToEntity(vehicleCreationDto);
        verify(vehicleRepository).save(vehicle);
        verify(vehicleMapper).vehicleEntityToVehicleResponseDto(vehicle);
    }

    @Test
    @DisplayName("Update an existing vehicle successfully")
    void updateExistingVehicleSuccessfully() {
        when(vehicleRepository.findById(ID_100)).thenReturn(Optional.of(vehicle));
        doNothing().when(vehicleMapper).update(vehicle, vehicleCreationDto);
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
        when(vehicleMapper.vehicleEntityToVehicleResponseDto(vehicle)).thenReturn(vehicleResponseDto);

        VehicleResponseDto result = vehicleService.update(vehicleCreationDto, ID_100);

        assertNotNull(result);
        assertEquals(vehicleResponseDto, result);
        verify(vehicleRepository).findById(ID_100);
        verify(vehicleMapper).update(vehicle, vehicleCreationDto);
        verify(vehicleRepository).save(vehicle);
        verify(vehicleMapper).vehicleEntityToVehicleResponseDto(vehicle);
    }

    @Test
    @DisplayName("Throw NoSuchElementException when updating non-existent vehicle")
    void throwExceptionWhenUpdatingNonExistentVehicle() {
        when(vehicleRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> vehicleService.update(vehicleCreationDto, NON_EXISTENT_ID));
        verify(vehicleRepository).findById(NON_EXISTENT_ID);
        verify(vehicleMapper, never()).update(any(), any());
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete an existing vehicle successfully")
    void deleteExistingVehicleSuccessfully() {
        when(vehicleRepository.existsById(ID_100)).thenReturn(true);
        doNothing().when(vehicleRepository).deleteById(ID_100);

        vehicleService.delete(ID_100);

        verify(vehicleRepository).existsById(ID_100);
        verify(vehicleRepository).deleteById(ID_100);
    }

    @Test
    @DisplayName("Throw NoSuchElementException when deleting non-existent vehicle")
    void throwExceptionWhenDeletingNonExistentVehicle() {
        when(vehicleRepository.existsById(NON_EXISTENT_ID)).thenReturn(false);

        assertThrows(NoSuchElementException.class,
                () -> vehicleService.delete(NON_EXISTENT_ID));
        verify(vehicleRepository).existsById(NON_EXISTENT_ID);
        verify(vehicleRepository, never()).deleteById(anyLong());
    }
}
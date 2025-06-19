package com.blablatwo.ride;

import com.blablatwo.city.City;
import com.blablatwo.city.CityDto;
import com.blablatwo.city.CityMapper;
import com.blablatwo.city.CityRepository;
import com.blablatwo.exceptions.NoSuchRideException;
import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.traveler.DriverProfileDto;
import com.blablatwo.traveler.Traveler;
import com.blablatwo.vehicle.Vehicle;
import com.blablatwo.vehicle.VehicleResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static com.blablatwo.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class RideServiceImplTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private RideMapper rideMapper;

    @Mock
    CityRepository cityRepository;

    @Mock
    CityMapper cityMapper;

    @InjectMocks
    private RideServiceImpl rideService;

    private Ride ride;
    private RideCreationDto rideCreationDTO;
    private RideResponseDto rideResponseDto;
    private City originCityEntity;
    private City destinationCityEntity;
    private CityDto originCityDto;
    private CityDto destinationCityDto;


    @BeforeEach
    void setUp() {
        // Initialize CityDto objects for clarity
        originCityDto = new CityDto(ID_ONE, CITY_NAME_ORIGIN);
        destinationCityDto = new CityDto(2L, CITY_NAME_DESTINATION);

        // Initialize City entities, assuming ID_ONE for origin and 2L for destination osmId
        originCityEntity = City.builder().id(ID_ONE).osmId(ID_ONE).name(CITY_NAME_ORIGIN).build();
        destinationCityEntity = City.builder().id(2L).osmId(2L).name(CITY_NAME_DESTINATION).build();

        // Initialize the Ride entity with the new osmId for City and the description field
        ride = Ride.builder()
                .id(ID_100)
                .driver(Traveler.builder().id(ID_ONE).username(TRAVELER_USERNAME_USER1).build())
                .origin(originCityEntity)
                .destination(destinationCityEntity)
                .departureTime(LOCAL_DATE_TIME)
                .availableSeats(ONE)
                .pricePerSeat(BIG_DECIMAL)
                .vehicle(Vehicle.builder().id(ID_ONE).licensePlate(VEHICLE_LICENSE_PLATE_1).build())
                .rideStatus(RideStatus.OPEN)
                .lastModified(INSTANT)
                .passengers(Collections.emptyList())
                .description(RIDE_DESCRIPTION) // New description field
                .build();

        // Initialize RideCreationDto with CityDto objects and the new description field
        rideCreationDTO = new RideCreationDto(
                ID_ONE,
                originCityDto, // Updated to use CityDto for origin
                destinationCityDto, // Updated to use CityDto for destination
                LOCAL_DATE_TIME,
                ONE,
                BIG_DECIMAL,
                ID_ONE,
                RIDE_DESCRIPTION // New description field
        );

        // Assuming RideResponseDto structure remains the same as provided,
        // without a description field, and CityResponseDto still only has id and name.
        rideResponseDto = new RideResponseDto(
                ID_100,
                new DriverProfileDto(ID_ONE, TRAVELER_USERNAME_USER1, null, null, null),
                originCityDto,
                destinationCityDto,
                LOCAL_DATE_TIME,
                ONE,
                BIG_DECIMAL,
                new VehicleResponseDto(ID_ONE, VEHICLE_MAKE_TESLA, VEHICLE_MODEL_MODEL_S, VEHICLE_PRODUCTION_YEAR_2021, VEHICLE_COLOR_RED, VEHICLE_LICENSE_PLATE_1),
                RideStatus.OPEN,
                INSTANT,
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Get ride by existing ID returns DTO")
    void getRideByIdReturnsDto() {
        // Arrange
        when(rideRepository.findById(ID_100)).thenReturn(Optional.of(ride));
        when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);

        // Act
        Optional<RideResponseDto> result = rideService.getById(ID_100);

        // Assert
        assertTrue(result.isPresent(), "Ride should be found by ID");
        assertEquals(rideResponseDto, result.get(), "Retrieved DTO should match the expected DTO");
    }

    @Test
    @DisplayName("Get ride by non-existent ID returns empty optional")
    void getRideByIdReturnsEmptyForNonExistentId() {
        // Arrange
        when(rideRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // Act
        Optional<RideResponseDto> result = rideService.getById(NON_EXISTENT_ID);

        // Assert
        assertFalse(result.isPresent(), "No ride should be found with non-existent ID");
    }

    @Test
    @DisplayName("Create a new ride successfully when cities already exist")
    void shouldCreateNewRideSuccessfullyWhenCitiesExist() {
        // Arrange
        // Simulate that the origin and destination cities already exist in the database
        when(cityRepository.findByOsmId(rideCreationDTO.origin().osmId())).thenReturn(Optional.of(originCityEntity));
        when(cityRepository.findByOsmId(rideCreationDTO.destination().osmId())).thenReturn(Optional.of(destinationCityEntity));

        when(rideMapper.rideCreationDtoToEntity(rideCreationDTO)).thenReturn(ride);
        when(rideRepository.save(ride)).thenReturn(ride);
        when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);

        // Act
        RideResponseDto result = rideService.create(rideCreationDTO);

        // Assert
        assertNotNull(result, "Resulting DTO should not be null");
        assertEquals(rideResponseDto, result, "Created ride DTO should match the expected DTO");

        // Verify interactions
        verify(cityRepository, times(1)).findByOsmId(rideCreationDTO.origin().osmId());
        verify(cityRepository, times(1)).findByOsmId(rideCreationDTO.destination().osmId());
        verify(cityRepository, never()).save(any(City.class)); // save should not be called if present
        verify(rideMapper).rideCreationDtoToEntity(rideCreationDTO);
        verify(rideRepository).save(ride);
        verify(rideMapper).rideEntityToRideResponseDto(ride);
    }

    @Test
    @DisplayName("Create a new ride successfully when cities do not exist and are saved")
    void shouldCreateNewRideSuccessfullyWhenCitiesDoNotExist() {
        // Arrange
        // Simulate that the origin and destination cities do not exist initially
        when(cityRepository.findByOsmId(rideCreationDTO.origin().osmId())).thenReturn(Optional.empty());
        when(cityRepository.findByOsmId(rideCreationDTO.destination().osmId())).thenReturn(Optional.empty());

        // Mock the behavior for saving new cities
        when(cityMapper.cityDtoToEntity(rideCreationDTO.origin())).thenReturn(originCityEntity);
        when(cityMapper.cityDtoToEntity(rideCreationDTO.destination())).thenReturn(destinationCityEntity);
        when(cityRepository.save(originCityEntity)).thenReturn(originCityEntity);
        when(cityRepository.save(destinationCityEntity)).thenReturn(destinationCityEntity);


        when(rideMapper.rideCreationDtoToEntity(rideCreationDTO)).thenReturn(ride);
        when(rideRepository.save(ride)).thenReturn(ride);
        when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);

        // Act
        RideResponseDto result = rideService.create(rideCreationDTO);

        // Assert
        assertNotNull(result, "Resulting DTO should not be null");
        assertEquals(rideResponseDto, result, "Created ride DTO should match the expected DTO");

        // Verify interactions
        verify(cityRepository, times(1)).findByOsmId(rideCreationDTO.origin().osmId());
        verify(cityRepository, times(1)).findByOsmId(rideCreationDTO.destination().osmId());
        verify(cityMapper, times(1)).cityDtoToEntity(rideCreationDTO.origin());
        verify(cityMapper, times(1)).cityDtoToEntity(rideCreationDTO.destination());
        verify(cityRepository, times(1)).save(originCityEntity); // save should be called if not present
        verify(cityRepository, times(1)).save(destinationCityEntity); // save should be called if not present
        verify(rideMapper).rideCreationDtoToEntity(rideCreationDTO);
        verify(rideRepository).save(ride);
        verify(rideMapper).rideEntityToRideResponseDto(ride);
    }

    @Test
    @DisplayName("Throw NoSuchRideException when updating a non-existent ride")
    void throwExceptionWhenUpdatingNonExistentRide() {
        // Arrange
        when(rideRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchRideException.class, () -> rideService.update(rideCreationDTO, NON_EXISTENT_ID),
                "Updating a non-existent ride should throw NoSuchRideException");
        verify(rideRepository).findById(NON_EXISTENT_ID);
        verify(rideMapper, never()).update(any(), any());
        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete an existing ride successfully")
    void deleteRideSuccessfully() {
        // Arrange
        when(rideRepository.existsById(ID_100)).thenReturn(true);
        doNothing().when(rideRepository).deleteById(ID_100);

        // Act
        rideService.delete(ID_100);

        // Assert
        verify(rideRepository).existsById(ID_100);
        verify(rideRepository).deleteById(ID_100);
    }

    @Test
    @DisplayName("Throw NoSuchRideException when deleting a non-existent ride")
    void throwExceptionWhenDeletingNonExistentRide() {
        // Arrange
        when(rideRepository.existsById(NON_EXISTENT_ID)).thenReturn(false);

        // Act & Assert
        assertThrows(NoSuchRideException.class, () -> rideService.delete(NON_EXISTENT_ID),
                "Deleting a non-existent ride should throw NoSuchRideException");
        verify(rideRepository).existsById(NON_EXISTENT_ID);
        verify(rideRepository, never()).deleteById(anyLong());
    }
}
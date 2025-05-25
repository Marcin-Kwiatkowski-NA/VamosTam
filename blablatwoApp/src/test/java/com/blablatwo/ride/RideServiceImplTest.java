package com.blablatwo.ride;

import com.blablatwo.city.City;
import com.blablatwo.city.CityResponseDto;
import com.blablatwo.exceptions.ETagMismatchException;
import com.blablatwo.exceptions.MissingETagHeaderException;
import com.blablatwo.exceptions.NoSuchRideException;
import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.traveler.DriverProfileDto;
import com.blablatwo.traveler.Traveler;
import com.blablatwo.vehicle.Vehicle;
import com.blablatwo.vehicle.VehicleResponseDto;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.blablatwo.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class RideServiceImplTest {

  @Mock
  private RideRepository rideRepository;

  @Mock
  private RideMapper rideMapper;

  @InjectMocks
  private RideServiceImpl rideService;

  private Ride ride;
  private RideCreationDto rideCreationDTO;
  private RideResponseDto rideResponseDto;

  @BeforeEach
  void setUp() {
    ride = Ride.builder()
            .id(ID_100)
            .driver(Traveler.builder().id(ID_ONE).username(TRAVELER_USERNAME_USER1).build())
            .origin(City.builder().id(ID_ONE).name(CITY_NAME_ORIGIN).build())
            .destination(City.builder().id(2L).name(CITY_NAME_DESTINATION).build())
            .departureTime(LOCAL_DATE_TIME)
            .availableSeats(ONE)
            .pricePerSeat(BIG_DECIMAL)
            .vehicle(Vehicle.builder().id(ID_ONE).licensePlate(VEHICLE_LICENSE_PLATE_1).build())
            .rideStatus(RideStatus.OPEN)
            .lastModified(INSTANT)
            .passengers(Collections.emptyList())
            .build();

    rideCreationDTO = new RideCreationDto(
            ID_ONE,
            CITY_NAME_ORIGIN,
            CITY_NAME_DESTINATION,
            LOCAL_DATE_TIME,
            ONE,
            BIG_DECIMAL,
            ID_ONE
    );

    rideResponseDto = new RideResponseDto(
            ID_100,
            new DriverProfileDto(ID_ONE, TRAVELER_USERNAME_USER1, null, null, null),
            new CityResponseDto(ID_ONE, CITY_NAME_ORIGIN),
            new CityResponseDto(2L, CITY_NAME_DESTINATION),
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
  @DisplayName("Create a new ride successfully")
  void shouldCreateNewRideSuccessfully() {
    // Arrange
    when(rideMapper.rideCreationDtoToEntity(rideCreationDTO)).thenReturn(ride);
    when(rideRepository.save(ride)).thenReturn(ride);
    when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);

    // Act
    RideResponseDto result = rideService.create(rideCreationDTO);

    // Assert
    assertNotNull(result, "Resulting DTO should not be null");
    assertEquals(rideResponseDto, result, "Created ride DTO should match the expected DTO");
    verify(rideRepository).save(ride);
    verify(rideMapper).rideCreationDtoToEntity(rideCreationDTO);
    verify(rideMapper).rideEntityToRideResponseDto(ride);
  }

  @Test
  @DisplayName("Update a ride's details successfully with matching ETag")
  void updateRideDetailsSuccessfullyWithMatchingETag() {
    // Arrange
    when(rideRepository.findById(ID_100)).thenReturn(Optional.of(ride));
    doNothing().when(rideMapper).update(ride, rideCreationDTO);
    when(rideRepository.save(ride)).thenReturn(ride);
    when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);

    // Act
    RideResponseDto result = rideService.update(rideCreationDTO, ID_100, ETAG);

    // Assert
    assertNotNull(result, "Resulting DTO should not be null");
    assertEquals(rideResponseDto, result, "Updated ride DTO should match the expected DTO");
    verify(rideRepository).findById(ID_100);
    verify(rideMapper).update(ride, rideCreationDTO);
    verify(rideRepository).save(ride);
    verify(rideMapper).rideEntityToRideResponseDto(ride);
  }

  @Test
  @DisplayName("Throw NoSuchRideException when updating a non-existent ride")
  void throwExceptionWhenUpdatingNonExistentRide() {
    // Arrange
    when(rideRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(NoSuchRideException.class, () -> rideService.update(rideCreationDTO, NON_EXISTENT_ID, ETAG),
            "Updating a non-existent ride should throw NoSuchRideException");
    verify(rideRepository).findById(NON_EXISTENT_ID);
    verify(rideMapper, never()).update(any(), any());
    verify(rideRepository, never()).save(any());
  }

  @Test
  @DisplayName("Throw MissingETagHeaderException when If-Match header is null")
  void throwExceptionWhenIfMatchHeaderIsNull() {
    // Arrange
    when(rideRepository.findById(ID_100)).thenReturn(Optional.of(ride));

    // Act & Assert
    assertThrows(MissingETagHeaderException.class, () -> rideService.update(rideCreationDTO, ID_100, null),
            "Missing If-Match header should throw MissingETagHeaderException");
    verify(rideRepository).findById(ID_100);
    verify(rideMapper, never()).update(any(), any());
    verify(rideRepository, never()).save(any());
  }

  @Test
  @DisplayName("Throw ETagMismatchException when If-Match header does not match")
  void throwExceptionWhenIfMatchHeaderDoesNotMatch() {
    // Arrange
    String nonMatchingEtag = "non-matching-etag";
    when(rideRepository.findById(ID_100)).thenReturn(Optional.of(ride));

    // Act & Assert
    assertThrows(ETagMismatchException.class, () -> rideService.update(rideCreationDTO, ID_100, nonMatchingEtag),
            "Non-matching If-Match header should throw ETagMismatchException");
    verify(rideRepository).findById(ID_100);
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
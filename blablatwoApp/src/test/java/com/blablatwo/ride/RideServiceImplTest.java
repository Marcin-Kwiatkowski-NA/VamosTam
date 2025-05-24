package com.blablatwo.ride;

import com.blablatwo.exceptions.ETagMismatchException;
import com.blablatwo.exceptions.MissingETagHeaderException;
import com.blablatwo.exceptions.NoSuchRideException;
import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
  RideResponseDto rideResponseDto;

  @BeforeEach
  void setUp() {
    ride = Ride.builder()
            .id(ID_100)
            .lastModified(INSTANT)
            .build();

    rideCreationDTO = new RideCreationDto(
            CITY_NAME_ORIGIN, CITY_NAME_DESTINATION, LOCAL_DATE_TIME, ONE, BIG_DECIMAL, ID_100
    );

    rideResponseDto = new RideResponseDto(
            ID_100, null, null, null, null, null, LOCAL_DATE_TIME,
            ONE, BIG_DECIMAL, null, INSTANT, null);
  }

  @Test
  @DisplayName("Return ride when finding by existing ID")
  void returnRideWhenFindingByExistingId() {
    // Arrange
    when(rideRepository.findById(ID_100)).thenReturn(Optional.of(ride));
    when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);

    // Act
    Optional<RideResponseDto> result = rideService.getById(ID_100);

    // Assert
    assertTrue(result.isPresent(), "Ride should be found by ID");
  }

  @Test
  @DisplayName("Return empty when finding by non-existent ID")
  void returnEmptyWhenFindingByNonExistentId() {
    // Arrange
    when(rideRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

    // Act
    Optional<RideResponseDto> result = rideService.getById(NON_EXISTENT_ID);

    // Assert
    assertFalse(result.isPresent(), "No ride should be found with non-existent ID");
  }

  @Test
  @DisplayName("Save a new ride successfully")
  void shouldSaveNewRideSuccessfully() {
    // Arrange
    when(rideMapper.rideCreationDtoToEntity(rideCreationDTO)).thenReturn(ride);
    when(rideRepository.save(ride)).thenReturn(ride);
    when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);

    // Act
    RideResponseDto result = rideService.create(rideCreationDTO);

    // Assert
    assertEquals(rideResponseDto, result, "Saved ride should match the expected ride");
    verify(rideRepository).save(ride);
  }

  @Test
  @DisplayName("Update a ride's details successfully")
  void updateRideDetailsSuccessfully() {
    // Arrange
    when(rideRepository.findById(ID_100)).thenReturn(Optional.of(ride));
    when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);
    doNothing().when(rideMapper).update(ride, rideCreationDTO);

    // Act
    RideResponseDto result = rideService.update(rideCreationDTO, ride.getId(), ETAG);

    // Assert
    assertEquals(rideResponseDto, result, "Updated ride should match the expected ride");
    verify(rideMapper).update(ride, rideCreationDTO);
    verify(rideRepository).save(ride);
  }

  @Test
  @DisplayName("Throw exception when updating a non-existent ride")
  void throwExceptionWhenUpdatingNonExistentRide() {
    // Arrange
    when(rideRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(NoSuchRideException.class, () -> rideService.update(rideCreationDTO, NON_EXISTENT_ID, "ifMatch"),
            "Updating non-existent ride should throw NoSuchRideException");
  }

  @Test
  @DisplayName("Throw exception when ETag header is missing in ifMatch")
  void throwExceptionWhenETagHeaderIsMissingInIfMatch() {
    // Arrange
    String ifMatch = null;

    // Act & Assert
    assertThrows(MissingETagHeaderException.class, () -> rideService.eTagCheck(ifMatch, ETAG),
            "Missing ETag header should throw MissingETagHeaderException");
  }

  @Test
  @DisplayName("Throw exception when ETag header is Different than existing")
  void throwExceptionWhenETagsDoNotMatch() {
    // Arrange
    String ifMatch = "null";

    // Act & Assert
    assertThrows(ETagMismatchException.class, () -> rideService.eTagCheck(ETAG, ifMatch),
            "Different ETag header should throw ETagMismatchException");
  }

  @Test
  @DisplayName("Delete a ride successfully")
  void deleteRideSuccessfully() {
    // Arrange
    when(rideRepository.existsById(anyLong())).thenReturn(true);
    doNothing().when(rideRepository).deleteById(anyLong());

    // Act
    rideService.delete(ID_100);

    // Assert
    verify(rideRepository).deleteById(ID_100);
  }

  @Test
  @DisplayName("Throw exception when deleting a non-existent ride")
  void throwExceptionWhenDeletingNonExistentRide() {
    // Arrange
    when(rideRepository.existsById(NON_EXISTENT_ID)).thenReturn(false);

    // Act & Assert
    assertThrows(NoSuchRideException.class, () -> rideService.delete(NON_EXISTENT_ID),
            "Deleting non-existent ride should throw NoSuchRideException");
  }
}

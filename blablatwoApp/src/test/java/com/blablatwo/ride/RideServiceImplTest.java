package com.blablatwo.ride;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.blablatwo.ride.DTO.RideResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.blablatwo.exceptions.MissingETagHeaderException;
import com.blablatwo.exceptions.NoSuchRideException;

import java.time.Instant;
import java.util.*;

import static com.blablatwo.util.Constants.*;


@ExtendWith(MockitoExtension.class)
class RideServiceImplTest {

  @Mock
  private RideRepository rideRepository;

  @Mock
  private RideMapper rideMapper;

  @Mock
  private RideEntity rideEntity;

  @Mock
  private RideResponseDto rideResponseDto;

  @Mock
  private RideCreationDTO rideCreationDTO;


  @InjectMocks
  private RideServiceImpl rideService;


  @Test
  @DisplayName("Return ride when finding by existing ID")
  void shouldReturnRideWhenFindingByExistingId() {
    // Arrange
    when(rideRepository.findById(anyLong())).thenReturn(Optional.of(rideEntity));
    when(rideMapper.rideEntityToRideResponseDto(rideEntity)).thenReturn(rideResponseDto);

    // Act
    Optional<RideResponseDto> result = rideService.getById(ID_100_L);

    // Assert
    assertTrue(result.isPresent(), "Ride should be found by ID");
    assertEquals(rideResponseDto, result.get(), "Ride details should match");
  }

  @Test
  @DisplayName("Return empty when finding by non-existent ID")
  void shouldReturnEmptyWhenFindingByNonExistentId() {
    // Arrange
    when(rideRepository.findById(NON_EXISTENT_ID_LONG)).thenReturn(Optional.empty());

    // Act
    Optional<RideResponseDto> result = rideService.getById(NON_EXISTENT_ID_LONG);

    // Assert
    assertFalse(result.isPresent(), "No ride should be found with non-existent ID");
  }

  @Test
  @DisplayName("Save a new ride successfully")
  void shouldSaveNewRideSuccessfully() {
    // Arrange
    when(rideMapper.rideCreationDtoToEntity(rideCreationDTO)).thenReturn(rideEntity);
    when(rideRepository.save(rideEntity)).thenReturn(rideEntity);
    when(rideMapper.rideEntityToRideResponseDto(rideEntity)).thenReturn(rideResponseDto);

    // Act
    RideResponseDto result = rideService.create(rideCreationDTO);

    // Assert
    assertEquals(rideResponseDto, result, "Saved ride should match the expected ride");
    verify(rideRepository).save(rideEntity);
  }

  @Test
  @DisplayName("Update a ride's details successfully")
  void shouldUpdateRideDetailsSuccessfully() {
    // Arrange
    when(rideRepository.findById(anyLong())).thenReturn(Optional.of(rideEntity));
    doNothing().when(rideMapper).update(rideEntity, rideCreationDTO);
    when(rideMapper.rideEntityToRideResponseDto(rideEntity)).thenReturn(rideResponseDto);

    // Act
    RideResponseDto result = rideService.update(rideCreationDTO, rideEntity.getId());

    // Assert
    assertEquals(rideResponseDto, result, "Updated ride should match the expected ride");
    verify(rideMapper).update(rideEntity, rideCreationDTO);
  }

  @Test
  @DisplayName("Throw exception when updating a non-existent ride")
  void shouldThrowExceptionWhenUpdatingNonExistentRide() {
    // Arrange
    when(rideRepository.findById(NON_EXISTENT_ID_LONG)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(NoSuchRideException.class, () -> rideService.update(rideCreationDTO, NON_EXISTENT_ID_LONG),
            "Updating non-existent ride should throw NoSuchRideException");
  }

  @Test
  @DisplayName("Return true when ETag matches in ifMatch")
  void shouldReturnTrueWhenETagMatchesInIfMatch() {
    // Arrange
    String ifMatch = INSTANT.toString();

    when(rideRepository.findById(anyLong())).thenReturn(Optional.of(rideEntity));
    when(rideMapper.rideEntityToRideResponseDto(rideEntity)).thenReturn(rideResponseDto);
    when(rideResponseDto.lastModified()).thenReturn(INSTANT);

    // Act
    boolean result = rideService.ifMatch(rideEntity.getId(), ifMatch);

    // Assert
    assertTrue(result, "ifMatch should return true when ETag matches");
  }

  @Test
  @DisplayName("Return false when ETag does not match in ifMatch")
  void shouldReturnFalseWhenETagDoesNotMatchInIfMatch() {
    // Arrange
    String ifMatch = INSTANT.minusMillis(ONE).toString(); // Different ETag

    when(rideRepository.findById(anyLong())).thenReturn(Optional.of(rideEntity));
    when(rideMapper.rideEntityToRideResponseDto(rideEntity)).thenReturn(rideResponseDto);
    when(rideResponseDto.lastModified()).thenReturn(INSTANT);

    // Act
    boolean result = rideService.ifMatch(rideEntity.getId(), ifMatch);

    // Assert
    assertFalse(result, "ifMatch should return false when ETag does not match");
  }

  @Test
  @DisplayName("Throw exception when ETag header is missing in ifMatch")
  void shouldThrowExceptionWhenETagHeaderIsMissingInIfMatch() {
    // Arrange
    String ifMatch = null;

    // Act & Assert
    assertThrows(MissingETagHeaderException.class, () -> rideService.ifMatch(1L, ifMatch),
            "Missing ETag header should throw MissingETagHeaderException");
  }

  @Test
  @DisplayName("Throw exception when ride does not exist in ifMatch")
  void shouldThrowExceptionWhenRideDoesNotExistInIfMatch() {
    // Arrange
    when(rideRepository.findById(NON_EXISTENT_ID_LONG)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(NoSuchRideException.class, () -> rideService.ifMatch(NON_EXISTENT_ID_LONG, "etag"),
            "Non-existent ride should throw NoSuchRideException in ifMatch");
  }

  @Test
  @DisplayName("Delete a ride successfully")
  void shouldDeleteRideSuccessfully() {
    // Arrange
    when(rideRepository.existsById(anyLong())).thenReturn(true);
    doNothing().when(rideRepository).deleteById(anyLong());

    // Act
    rideService.delete(ID_100_L);

    // Assert
    verify(rideRepository).deleteById(ID_100_L);
  }

  @Test
  @DisplayName("Throw exception when deleting a non-existent ride")
  void shouldThrowExceptionWhenDeletingNonExistentRide() {
    // Arrange
    when(rideRepository.existsById(NON_EXISTENT_ID_LONG)).thenReturn(false);

    // Act & Assert
    assertThrows(NoSuchRideException.class, () -> rideService.delete(NON_EXISTENT_ID_LONG),
            "Deleting non-existent ride should throw NoSuchRideException");
  }
}

package com.blablatwo.ride;

import com.blablatwo.city.City;
import com.blablatwo.city.CityDto;
import com.blablatwo.city.CityMapper;
import com.blablatwo.city.CityResolutionService;
import com.blablatwo.domain.Status;
import com.blablatwo.exceptions.AlreadyBookedException;
import com.blablatwo.exceptions.BookingNotFoundException;
import com.blablatwo.exceptions.NoSuchRideException;
import com.blablatwo.exceptions.RideNotBookableException;
import com.blablatwo.exceptions.SegmentFullException;
import com.blablatwo.ride.dto.BookRideRequest;
import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.ride.dto.RideSearchCriteriaDto;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import com.blablatwo.user.capability.CapabilityService;
import com.blablatwo.user.exception.NoSuchUserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.blablatwo.util.Constants.*;
import static com.blablatwo.util.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;


@ExtendWith(MockitoExtension.class)
class RideServiceImplTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private RideBookingRepository bookingRepository;

    @Mock
    private RideMapper rideMapper;

    @Mock
    CityResolutionService cityResolutionService;

    @Mock
    CityMapper cityMapper;

    @Mock
    UserAccountRepository userAccountRepository;

    @Mock
    RideResponseEnricher rideResponseEnricher;

    @Mock
    CapabilityService capabilityService;

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
        lenient().when(rideResponseEnricher.enrich(any(Ride.class), any(RideResponseDto.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(rideResponseEnricher.enrich(anyList(), anyList()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        originCityDto = originCityDto();
        destinationCityDto = destinationCityDto();
        originCityEntity = anOriginCity().build();
        destinationCityEntity = aDestinationCity().build();
        ride = buildRideWithStops(originCityEntity, destinationCityEntity);
        rideCreationDTO = aRideCreationDto().build();
        rideResponseDto = aRideResponseDto().build();
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
    @DisplayName("Create a new ride successfully using CityResolutionService")
    void shouldCreateNewRideSuccessfully() {
        // Arrange
        UserAccount driver = ride.getDriver();
        Long driverId = driver.getId();
        when(capabilityService.canCreateRide(driverId)).thenReturn(true);
        when(userAccountRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(cityResolutionService.resolveCityByPlaceId(rideCreationDTO.originPlaceId(), "pl"))
                .thenReturn(originCityEntity);
        when(cityResolutionService.resolveCityByPlaceId(rideCreationDTO.destinationPlaceId(), "pl"))
                .thenReturn(destinationCityEntity);

        when(rideMapper.rideCreationDtoToEntity(rideCreationDTO)).thenReturn(ride);
        when(rideRepository.save(ride)).thenReturn(ride);
        when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);

        // Act
        RideResponseDto result = rideService.createForCurrentUser(rideCreationDTO, driverId);

        // Assert
        assertNotNull(result, "Resulting DTO should not be null");
        assertEquals(rideResponseDto, result, "Created ride DTO should match the expected DTO");

        // Verify interactions
        verify(capabilityService).canCreateRide(driverId);
        verify(userAccountRepository).findById(driverId);
        verify(cityResolutionService, times(1)).resolveCityByPlaceId(rideCreationDTO.originPlaceId(), "pl");
        verify(cityResolutionService, times(1)).resolveCityByPlaceId(rideCreationDTO.destinationPlaceId(), "pl");
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

    @Nested
    @DisplayName("Search Rides Tests")
    class SearchRidesTests {

        @Test
        @DisplayName("Search rides returns paginated results")
        void searchRides_ReturnsPagedResults() {
            // Arrange - using placeId-based search criteria
            RideSearchCriteriaDto criteria = new RideSearchCriteriaDto(
                    ID_ONE, 2L, "pl", LocalDate.now(), null, null, 1
            );
            Pageable pageable = PageRequest.of(0, 10);
            Page<Ride> ridePage = new PageImpl<>(List.of(ride));

            when(rideRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(ridePage);
            when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);
            when(cityMapper.cityEntityToCityDto(originCityEntity, "pl")).thenReturn(originCityDto);
            when(cityMapper.cityEntityToCityDto(destinationCityEntity, "pl")).thenReturn(destinationCityDto);

            // Act
            Page<RideResponseDto> result = rideService.searchRides(criteria, pageable);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            verify(rideRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Search rides with null criteria returns results")
        void searchRides_WithNullCriteria_ReturnsResults() {
            // Arrange
            RideSearchCriteriaDto criteria = new RideSearchCriteriaDto(null, null, null, null, null, null, 1);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Ride> ridePage = new PageImpl<>(List.of(ride));

            when(rideRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(ridePage);
            when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);
            when(cityMapper.cityEntityToCityDto(originCityEntity, "pl")).thenReturn(originCityDto);
            when(cityMapper.cityEntityToCityDto(destinationCityEntity, "pl")).thenReturn(destinationCityDto);

            // Act
            Page<RideResponseDto> result = rideService.searchRides(criteria, pageable);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
        }

        @Test
        @DisplayName("Search rides with specific time returns results")
        void searchRides_WithSpecificTime_ReturnsResults() {
            // Arrange
            LocalTime searchTime = LocalTime.of(14, 0);
            RideSearchCriteriaDto criteria = new RideSearchCriteriaDto(
                    ID_ONE, 2L, "en", LocalDate.now().plusDays(1), null, searchTime, 1
            );
            Pageable pageable = PageRequest.of(0, 10);
            Page<Ride> ridePage = new PageImpl<>(List.of(ride));

            when(rideRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(ridePage);
            when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);
            when(cityMapper.cityEntityToCityDto(originCityEntity, "en")).thenReturn(originCityDto);
            when(cityMapper.cityEntityToCityDto(destinationCityEntity, "en")).thenReturn(destinationCityDto);

            // Act
            Page<RideResponseDto> result = rideService.searchRides(criteria, pageable);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            verify(rideRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Search rides for future date without time uses start of day")
        void searchRides_FutureDateWithoutTime_UsesStartOfDay() {
            // Arrange
            RideSearchCriteriaDto criteria = new RideSearchCriteriaDto(
                    ID_ONE, 2L, "pl", LocalDate.now().plusDays(5), null, null, 1
            );
            Pageable pageable = PageRequest.of(0, 10);
            Page<Ride> ridePage = new PageImpl<>(List.of(ride));

            when(rideRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(ridePage);
            when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);
            when(cityMapper.cityEntityToCityDto(originCityEntity, "pl")).thenReturn(originCityDto);
            when(cityMapper.cityEntityToCityDto(destinationCityEntity, "pl")).thenReturn(destinationCityDto);

            // Act
            Page<RideResponseDto> result = rideService.searchRides(criteria, pageable);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
        }

        @Test
        @DisplayName("Get all rides returns paginated results")
        void getAllRides_ReturnsPagedResults() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Ride> ridePage = new PageImpl<>(List.of(ride));

            when(rideRepository.findAll(pageable)).thenReturn(ridePage);
            when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);

            // Act
            Page<RideResponseDto> result = rideService.getAllRides(pageable);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            verify(rideRepository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("Book Ride Tests")
    class BookRideTests {

        private UserAccount passenger;
        private Ride bookableRide;
        private BookRideRequest bookRequest;

        @BeforeEach
        void setUpBooking() {
            passenger = aPassengerAccount().build();
            bookableRide = aRide(originCityEntity, destinationCityEntity)
                    .totalSeats(3)
                    .vehicle(null)
                    .source(RideSource.INTERNAL)
                    .build();
            bookableRide.setStops(new ArrayList<>(buildStops(bookableRide, originCityEntity, destinationCityEntity)));
            bookableRide.setBookings(new ArrayList<>());
            bookRequest = aBookRideRequest().build();
        }

        @Test
        @DisplayName("Book ride successfully creates a booking")
        void bookRide_Success() {
            // Arrange
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookableRide));
            when(capabilityService.canBook(2L)).thenReturn(true);
            when(bookingRepository.existsByRideIdAndPassengerId(ID_100, 2L)).thenReturn(false);
            when(userAccountRepository.findById(2L)).thenReturn(Optional.of(passenger));
            when(bookingRepository.save(any(RideBooking.class))).thenAnswer(inv -> inv.getArgument(0));
            when(rideRepository.save(any(Ride.class))).thenReturn(bookableRide);
            when(rideMapper.rideEntityToRideResponseDto(any())).thenReturn(rideResponseDto);

            // Act
            RideResponseDto result = rideService.bookRide(ID_100, 2L, bookRequest);

            // Assert
            assertNotNull(result);
            verify(bookingRepository).save(any(RideBooking.class));
            verify(rideRepository).save(bookableRide);
        }

        @Test
        @DisplayName("Book ride throws exception when segment is full")
        void bookRide_ThrowsWhenSegmentFull() {
            // Arrange - fill all seats with existing bookings
            RideStop originStop = bookableRide.getStops().get(0);
            RideStop destStop = bookableRide.getStops().get(1);
            for (int i = 0; i < 3; i++) {
                bookableRide.getBookings().add(RideBooking.builder()
                        .ride(bookableRide).passenger(aDriverAccount().id((long) (10 + i)).build())
                        .boardStop(originStop).alightStop(destStop)
                        .bookedAt(Instant.now()).build());
            }
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookableRide));
            when(capabilityService.canBook(2L)).thenReturn(true);
            when(bookingRepository.existsByRideIdAndPassengerId(ID_100, 2L)).thenReturn(false);
            when(userAccountRepository.findById(2L)).thenReturn(Optional.of(passenger));

            // Act & Assert
            assertThrows(RideNotBookableException.class, () -> rideService.bookRide(ID_100, 2L, bookRequest));
        }

        @Test
        @DisplayName("Book ride throws exception when already booked")
        void bookRide_ThrowsWhenAlreadyBooked() {
            // Arrange
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookableRide));
            when(capabilityService.canBook(2L)).thenReturn(true);
            when(bookingRepository.existsByRideIdAndPassengerId(ID_100, 2L)).thenReturn(true);

            // Act & Assert
            assertThrows(AlreadyBookedException.class, () -> rideService.bookRide(ID_100, 2L, bookRequest));
        }

        @Test
        @DisplayName("Book ride throws exception when ride is not OPEN")
        void bookRide_ThrowsWhenNotOpen() {
            // Arrange
            bookableRide.setStatus(Status.CANCELLED);
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookableRide));
            when(capabilityService.canBook(2L)).thenReturn(true);
            when(bookingRepository.existsByRideIdAndPassengerId(ID_100, 2L)).thenReturn(false);
            when(userAccountRepository.findById(2L)).thenReturn(Optional.of(passenger));

            // Act & Assert
            assertThrows(RideNotBookableException.class, () -> rideService.bookRide(ID_100, 2L, bookRequest));
        }

        @Test
        @DisplayName("Book ride throws exception when ride not found")
        void bookRide_ThrowsWhenRideNotFound() {
            // Arrange
            when(rideRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(NoSuchRideException.class, () -> rideService.bookRide(NON_EXISTENT_ID, 2L, bookRequest));
        }

        @Test
        @DisplayName("Book ride throws exception when user not found")
        void bookRide_ThrowsWhenUserNotFound() {
            // Arrange
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookableRide));
            when(capabilityService.canBook(NON_EXISTENT_ID)).thenReturn(true);
            when(bookingRepository.existsByRideIdAndPassengerId(ID_100, NON_EXISTENT_ID)).thenReturn(false);
            when(userAccountRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(NoSuchUserException.class, () -> rideService.bookRide(ID_100, NON_EXISTENT_ID, bookRequest));
        }
    }

    @Nested
    @DisplayName("Cancel Booking Tests")
    class CancelBookingTests {

        private UserAccount passenger;
        private Ride bookedRide;
        private RideBooking existingBooking;

        @BeforeEach
        void setUpCancellation() {
            passenger = aPassengerAccount().build();
            bookedRide = aRide(originCityEntity, destinationCityEntity)
                    .totalSeats(3)
                    .vehicle(null)
                    .source(RideSource.INTERNAL)
                    .build();
            bookedRide.setStops(new ArrayList<>(buildStops(bookedRide, originCityEntity, destinationCityEntity)));
            bookedRide.setBookings(new ArrayList<>());

            existingBooking = RideBooking.builder()
                    .ride(bookedRide)
                    .passenger(passenger)
                    .boardStop(bookedRide.getStops().get(0))
                    .alightStop(bookedRide.getStops().get(1))
                    .bookedAt(Instant.now())
                    .build();
            bookedRide.getBookings().add(existingBooking);
        }

        @Test
        @DisplayName("Cancel booking successfully removes booking")
        void cancelBooking_Success() {
            // Arrange
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookedRide));
            when(userAccountRepository.existsById(2L)).thenReturn(true);
            when(bookingRepository.findByRideIdAndPassengerId(ID_100, 2L))
                    .thenReturn(Optional.of(existingBooking));
            when(rideRepository.save(any(Ride.class))).thenReturn(bookedRide);
            when(rideMapper.rideEntityToRideResponseDto(any())).thenReturn(rideResponseDto);

            // Act
            RideResponseDto result = rideService.cancelBooking(ID_100, 2L);

            // Assert
            assertNotNull(result);
            verify(bookingRepository).delete(existingBooking);
            verify(rideRepository).save(bookedRide);
        }

        @Test
        @DisplayName("Cancel booking throws exception when booking not found")
        void cancelBooking_ThrowsWhenBookingNotFound() {
            // Arrange
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookedRide));
            when(userAccountRepository.existsById(2L)).thenReturn(true);
            when(bookingRepository.findByRideIdAndPassengerId(ID_100, 2L))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(BookingNotFoundException.class, () -> rideService.cancelBooking(ID_100, 2L));
        }

        @Test
        @DisplayName("Cancel booking throws exception when ride not found")
        void cancelBooking_ThrowsWhenRideNotFound() {
            // Arrange
            when(rideRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(NoSuchRideException.class, () -> rideService.cancelBooking(NON_EXISTENT_ID, 2L));
        }
    }

    @Nested
    @DisplayName("Get Rides For Passenger Tests")
    class GetRidesForPassengerTests {

        @Test
        @DisplayName("Get rides for passenger returns list of rides")
        void getRidesForPassenger_ReturnsList() {
            // Arrange
            RideBooking booking = RideBooking.builder()
                    .ride(ride)
                    .passenger(aPassengerAccount().build())
                    .boardStop(ride.getStops().get(0))
                    .alightStop(ride.getStops().get(1))
                    .bookedAt(Instant.now())
                    .build();
            when(userAccountRepository.existsById(2L)).thenReturn(true);
            when(bookingRepository.findByPassengerId(2L)).thenReturn(List.of(booking));
            when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);

            // Act
            List<RideResponseDto> result = rideService.getRidesForPassenger(2L);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            verify(bookingRepository).findByPassengerId(2L);
        }

        @Test
        @DisplayName("Get rides for passenger returns empty list when no bookings")
        void getRidesForPassenger_ReturnsEmptyList() {
            // Arrange
            when(userAccountRepository.existsById(2L)).thenReturn(true);
            when(bookingRepository.findByPassengerId(2L)).thenReturn(Collections.emptyList());

            // Act
            List<RideResponseDto> result = rideService.getRidesForPassenger(2L);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Get rides for passenger throws exception when user not found")
        void getRidesForPassenger_ThrowsWhenUserNotFound() {
            // Arrange
            when(userAccountRepository.existsById(NON_EXISTENT_ID)).thenReturn(false);

            // Act & Assert
            assertThrows(NoSuchUserException.class, () -> rideService.getRidesForPassenger(NON_EXISTENT_ID));
        }
    }
}

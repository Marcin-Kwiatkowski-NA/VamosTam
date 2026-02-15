package com.blablatwo.ride;

import com.blablatwo.domain.Status;
import com.blablatwo.exceptions.AlreadyBookedException;
import com.blablatwo.exceptions.BookingNotFoundException;
import com.blablatwo.exceptions.NoSuchRideException;
import com.blablatwo.exceptions.RideNotBookableException;
import com.blablatwo.exceptions.SegmentFullException;
import com.blablatwo.location.Location;
import com.blablatwo.location.LocationDto;
import com.blablatwo.location.LocationResolutionService;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceImplTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private RideBookingRepository bookingRepository;

    @Mock
    private RideMapper rideMapper;

    @Mock
    LocationResolutionService locationResolutionService;

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
    private Location originLocation;
    private Location destinationLocation;

    @BeforeEach
    void setUp() {
        lenient().when(rideResponseEnricher.enrich(any(Ride.class), any(RideResponseDto.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(rideResponseEnricher.enrich(anyList(), anyList()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        originLocation = anOriginLocation().build();
        destinationLocation = aDestinationLocation().build();
        ride = buildRideWithStops(originLocation, destinationLocation);
        rideCreationDTO = aRideCreationDto().build();
        rideResponseDto = aRideResponseDto().build();
    }

    @Test
    @DisplayName("Get ride by existing ID returns DTO")
    void getRideByIdReturnsDto() {
        when(rideRepository.findById(ID_100)).thenReturn(Optional.of(ride));
        when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);

        Optional<RideResponseDto> result = rideService.getById(ID_100);

        assertTrue(result.isPresent());
        assertEquals(rideResponseDto, result.get());
    }

    @Test
    @DisplayName("Get ride by non-existent ID returns empty optional")
    void getRideByIdReturnsEmptyForNonExistentId() {
        when(rideRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        Optional<RideResponseDto> result = rideService.getById(NON_EXISTENT_ID);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Create a new ride successfully using LocationResolutionService")
    void shouldCreateNewRideSuccessfully() {
        UserAccount driver = ride.getDriver();
        Long driverId = driver.getId();
        when(capabilityService.canCreateRide(driverId)).thenReturn(true);
        when(userAccountRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(locationResolutionService.resolve(rideCreationDTO.origin())).thenReturn(originLocation);
        when(locationResolutionService.resolve(rideCreationDTO.destination())).thenReturn(destinationLocation);

        when(rideMapper.rideCreationDtoToEntity(rideCreationDTO)).thenReturn(ride);
        when(rideRepository.save(ride)).thenReturn(ride);
        when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);

        RideResponseDto result = rideService.createForCurrentUser(rideCreationDTO, driverId);

        assertNotNull(result);
        assertEquals(rideResponseDto, result);

        verify(capabilityService).canCreateRide(driverId);
        verify(userAccountRepository).findById(driverId);
        verify(locationResolutionService).resolve(rideCreationDTO.origin());
        verify(locationResolutionService).resolve(rideCreationDTO.destination());
        verify(rideMapper).rideCreationDtoToEntity(rideCreationDTO);
        verify(rideRepository).save(ride);
        verify(rideMapper).rideEntityToRideResponseDto(ride);
    }

    @Test
    @DisplayName("Throw NoSuchRideException when updating a non-existent ride")
    void throwExceptionWhenUpdatingNonExistentRide() {
        when(rideRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        assertThrows(NoSuchRideException.class, () -> rideService.update(rideCreationDTO, NON_EXISTENT_ID));
        verify(rideRepository).findById(NON_EXISTENT_ID);
        verify(rideMapper, never()).update(any(), any());
        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete an existing ride successfully")
    void deleteRideSuccessfully() {
        when(rideRepository.existsById(ID_100)).thenReturn(true);
        doNothing().when(rideRepository).deleteById(ID_100);

        rideService.delete(ID_100);

        verify(rideRepository).existsById(ID_100);
        verify(rideRepository).deleteById(ID_100);
    }

    @Test
    @DisplayName("Throw NoSuchRideException when deleting a non-existent ride")
    void throwExceptionWhenDeletingNonExistentRide() {
        when(rideRepository.existsById(NON_EXISTENT_ID)).thenReturn(false);

        assertThrows(NoSuchRideException.class, () -> rideService.delete(NON_EXISTENT_ID));
        verify(rideRepository).existsById(NON_EXISTENT_ID);
        verify(rideRepository, never()).deleteById(anyLong());
    }

    @Nested
    @DisplayName("Search Rides Tests")
    class SearchRidesTests {

        @Test
        @DisplayName("Search rides returns paginated results")
        void searchRides_ReturnsPagedResults() {
            RideSearchCriteriaDto criteria = new RideSearchCriteriaDto(
                    OSM_ID_ORIGIN, OSM_ID_DESTINATION,
                    null, null, null, null, null, null, null,
                    LocalDate.now(), null, null, 1
            );
            Pageable pageable = PageRequest.of(0, 10);
            Page<Ride> ridePage = new PageImpl<>(List.of(ride));

            when(rideRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(ridePage);
            when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);

            Page<RideResponseDto> result = rideService.searchRides(criteria, pageable);

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            verify(rideRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Search rides with null criteria returns results")
        void searchRides_WithNullCriteria_ReturnsResults() {
            RideSearchCriteriaDto criteria = new RideSearchCriteriaDto(null, null, null, null, null, null, null, null, null, null, null, null, 1);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Ride> ridePage = new PageImpl<>(List.of(ride));

            when(rideRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(ridePage);
            when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);

            Page<RideResponseDto> result = rideService.searchRides(criteria, pageable);

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
        }

        @Test
        @DisplayName("Search rides with specific time returns results")
        void searchRides_WithSpecificTime_ReturnsResults() {
            LocalTime searchTime = LocalTime.of(14, 0);
            RideSearchCriteriaDto criteria = new RideSearchCriteriaDto(
                    OSM_ID_ORIGIN, OSM_ID_DESTINATION,
                    null, null, null, null, null, null, null,
                    LocalDate.now().plusDays(1), null, searchTime, 1
            );
            Pageable pageable = PageRequest.of(0, 10);
            Page<Ride> ridePage = new PageImpl<>(List.of(ride));

            when(rideRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(ridePage);
            when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);

            Page<RideResponseDto> result = rideService.searchRides(criteria, pageable);

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            verify(rideRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Search rides for future date without time uses start of day")
        void searchRides_FutureDateWithoutTime_UsesStartOfDay() {
            RideSearchCriteriaDto criteria = new RideSearchCriteriaDto(
                    OSM_ID_ORIGIN, OSM_ID_DESTINATION,
                    null, null, null, null, null, null, null,
                    LocalDate.now().plusDays(5), null, null, 1
            );
            Pageable pageable = PageRequest.of(0, 10);
            Page<Ride> ridePage = new PageImpl<>(List.of(ride));

            when(rideRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(ridePage);
            when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);

            Page<RideResponseDto> result = rideService.searchRides(criteria, pageable);

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
        }

        @Test
        @DisplayName("Get all rides returns paginated results")
        void getAllRides_ReturnsPagedResults() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Ride> ridePage = new PageImpl<>(List.of(ride));

            when(rideRepository.findAll(pageable)).thenReturn(ridePage);
            when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);

            Page<RideResponseDto> result = rideService.getAllRides(pageable);

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
            bookableRide = aRide(originLocation, destinationLocation)
                    .totalSeats(3)
                    .vehicle(null)
                    .source(RideSource.INTERNAL)
                    .build();
            bookableRide.setStops(new ArrayList<>(buildStops(bookableRide, originLocation, destinationLocation)));
            bookableRide.setBookings(new ArrayList<>());
            bookRequest = aBookRideRequest().build();
        }

        @Test
        @DisplayName("Book ride successfully creates a booking")
        void bookRide_Success() {
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookableRide));
            when(capabilityService.canBook(2L)).thenReturn(true);
            when(bookingRepository.existsByRideIdAndPassengerId(ID_100, 2L)).thenReturn(false);
            when(userAccountRepository.findById(2L)).thenReturn(Optional.of(passenger));
            when(bookingRepository.save(any(RideBooking.class))).thenAnswer(inv -> inv.getArgument(0));
            when(rideRepository.save(any(Ride.class))).thenReturn(bookableRide);
            when(rideMapper.rideEntityToRideResponseDto(any())).thenReturn(rideResponseDto);

            RideResponseDto result = rideService.bookRide(ID_100, 2L, bookRequest);

            assertNotNull(result);
            verify(bookingRepository).save(any(RideBooking.class));
            verify(rideRepository).save(bookableRide);
        }

        @Test
        @DisplayName("Book ride throws exception when segment is full")
        void bookRide_ThrowsWhenSegmentFull() {
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

            assertThrows(RideNotBookableException.class, () -> rideService.bookRide(ID_100, 2L, bookRequest));
        }

        @Test
        @DisplayName("Book ride throws exception when already booked")
        void bookRide_ThrowsWhenAlreadyBooked() {
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookableRide));
            when(capabilityService.canBook(2L)).thenReturn(true);
            when(bookingRepository.existsByRideIdAndPassengerId(ID_100, 2L)).thenReturn(true);

            assertThrows(AlreadyBookedException.class, () -> rideService.bookRide(ID_100, 2L, bookRequest));
        }

        @Test
        @DisplayName("Book ride throws exception when ride is not OPEN")
        void bookRide_ThrowsWhenNotOpen() {
            bookableRide.setStatus(Status.CANCELLED);
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookableRide));
            when(capabilityService.canBook(2L)).thenReturn(true);
            when(bookingRepository.existsByRideIdAndPassengerId(ID_100, 2L)).thenReturn(false);
            when(userAccountRepository.findById(2L)).thenReturn(Optional.of(passenger));

            assertThrows(RideNotBookableException.class, () -> rideService.bookRide(ID_100, 2L, bookRequest));
        }

        @Test
        @DisplayName("Book ride throws exception when ride not found")
        void bookRide_ThrowsWhenRideNotFound() {
            when(rideRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

            assertThrows(NoSuchRideException.class, () -> rideService.bookRide(NON_EXISTENT_ID, 2L, bookRequest));
        }

        @Test
        @DisplayName("Book ride throws exception when user not found")
        void bookRide_ThrowsWhenUserNotFound() {
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookableRide));
            when(capabilityService.canBook(NON_EXISTENT_ID)).thenReturn(true);
            when(bookingRepository.existsByRideIdAndPassengerId(ID_100, NON_EXISTENT_ID)).thenReturn(false);
            when(userAccountRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

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
            bookedRide = aRide(originLocation, destinationLocation)
                    .totalSeats(3)
                    .vehicle(null)
                    .source(RideSource.INTERNAL)
                    .build();
            bookedRide.setStops(new ArrayList<>(buildStops(bookedRide, originLocation, destinationLocation)));
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
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookedRide));
            when(userAccountRepository.existsById(2L)).thenReturn(true);
            when(bookingRepository.findByRideIdAndPassengerId(ID_100, 2L))
                    .thenReturn(Optional.of(existingBooking));
            when(rideRepository.save(any(Ride.class))).thenReturn(bookedRide);
            when(rideMapper.rideEntityToRideResponseDto(any())).thenReturn(rideResponseDto);

            RideResponseDto result = rideService.cancelBooking(ID_100, 2L);

            assertNotNull(result);
            verify(bookingRepository).delete(existingBooking);
            verify(rideRepository).save(bookedRide);
        }

        @Test
        @DisplayName("Cancel booking throws exception when booking not found")
        void cancelBooking_ThrowsWhenBookingNotFound() {
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookedRide));
            when(userAccountRepository.existsById(2L)).thenReturn(true);
            when(bookingRepository.findByRideIdAndPassengerId(ID_100, 2L))
                    .thenReturn(Optional.empty());

            assertThrows(BookingNotFoundException.class, () -> rideService.cancelBooking(ID_100, 2L));
        }

        @Test
        @DisplayName("Cancel booking throws exception when ride not found")
        void cancelBooking_ThrowsWhenRideNotFound() {
            when(rideRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

            assertThrows(NoSuchRideException.class, () -> rideService.cancelBooking(NON_EXISTENT_ID, 2L));
        }
    }

    @Nested
    @DisplayName("Get Rides For Passenger Tests")
    class GetRidesForPassengerTests {

        @Test
        @DisplayName("Get rides for passenger returns list of rides")
        void getRidesForPassenger_ReturnsList() {
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

            List<RideResponseDto> result = rideService.getRidesForPassenger(2L);

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(bookingRepository).findByPassengerId(2L);
        }

        @Test
        @DisplayName("Get rides for passenger returns empty list when no bookings")
        void getRidesForPassenger_ReturnsEmptyList() {
            when(userAccountRepository.existsById(2L)).thenReturn(true);
            when(bookingRepository.findByPassengerId(2L)).thenReturn(Collections.emptyList());

            List<RideResponseDto> result = rideService.getRidesForPassenger(2L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Get rides for passenger throws exception when user not found")
        void getRidesForPassenger_ThrowsWhenUserNotFound() {
            when(userAccountRepository.existsById(NON_EXISTENT_ID)).thenReturn(false);

            assertThrows(NoSuchUserException.class, () -> rideService.getRidesForPassenger(NON_EXISTENT_ID));
        }
    }
}

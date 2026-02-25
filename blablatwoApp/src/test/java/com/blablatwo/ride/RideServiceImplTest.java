package com.blablatwo.ride;

import com.blablatwo.domain.Status;
import com.blablatwo.exceptions.NoSuchRideException;
import com.blablatwo.exceptions.NotRideDriverException;
import com.blablatwo.exceptions.RideHasBookingsException;
import com.blablatwo.location.Location;
import com.blablatwo.location.LocationResolutionService;
import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.ride.dto.RideSearchCriteriaDto;
import com.blablatwo.ride.event.BookingCancelledEvent;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import com.blablatwo.user.capability.CapabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
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

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    RideArrivalEstimator arrivalEstimator;

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

        assertThrows(NoSuchRideException.class, () -> rideService.update(rideCreationDTO, NON_EXISTENT_ID, ID_ONE));
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
                    null, null, null, null, null,
                    Instant.now(), null, 1
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
        @DisplayName("Search rides with minimal criteria returns results")
        void searchRides_WithMinimalCriteria_ReturnsResults() {
            RideSearchCriteriaDto criteria = new RideSearchCriteriaDto(
                    null, null, null, null, null, null, null,
                    Instant.now(), null, 1
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
        @DisplayName("Search rides with time window returns results")
        void searchRides_WithTimeWindow_ReturnsResults() {
            Instant earliest = FUTURE_DEPARTURE;
            Instant latest = earliest.plus(java.time.Duration.ofHours(12));
            RideSearchCriteriaDto criteria = new RideSearchCriteriaDto(
                    OSM_ID_ORIGIN, OSM_ID_DESTINATION,
                    null, null, null, null, null,
                    earliest, latest, 1
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
        @DisplayName("Search rides with future departure returns results")
        void searchRides_FutureDeparture_ReturnsResults() {
            RideSearchCriteriaDto criteria = new RideSearchCriteriaDto(
                    OSM_ID_ORIGIN, OSM_ID_DESTINATION,
                    null, null, null, null, null,
                    FUTURE_DEPARTURE, null, 1
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
    @DisplayName("Cancel Ride Tests")
    class CancelRideTests {

        private Ride cancelableRide;

        @BeforeEach
        void setUpCancelRide() {
            cancelableRide = buildRideWithStops(originLocation, destinationLocation);
            cancelableRide.setStatus(Status.ACTIVE);
        }

        @Test
        @DisplayName("Cancel ride with active bookings throws RideHasBookingsException")
        void cancelRide_BlockedByActiveBookings() {
            UserAccount passenger = aPassengerAccount().build();
            RideBooking activeBooking = aBooking(cancelableRide, passenger)
                    .status(BookingStatus.CONFIRMED).build();
            cancelableRide.setBookings(new ArrayList<>(List.of(activeBooking)));

            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(cancelableRide));

            assertThrows(RideHasBookingsException.class,
                    () -> rideService.cancelRide(ID_100, ID_ONE));
        }

        @Test
        @DisplayName("Cancel ride with no active bookings sets status to CANCELLED")
        void cancelRide_NoActiveBookings() {
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(cancelableRide));

            rideService.cancelRide(ID_100, ID_ONE);

            assertEquals(Status.CANCELLED, cancelableRide.getStatus());
        }

        @Test
        @DisplayName("Throws NoSuchRideException when ride not found")
        void cancelRide_ThrowsWhenRideNotFound() {
            when(rideRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

            assertThrows(NoSuchRideException.class,
                    () -> rideService.cancelRide(NON_EXISTENT_ID, ID_ONE));
        }

        @Test
        @DisplayName("Throws NotRideDriverException when caller is not the driver")
        void cancelRide_ThrowsWhenNotDriver() {
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(cancelableRide));

            assertThrows(NotRideDriverException.class,
                    () -> rideService.cancelRide(ID_100, 999L));
        }
    }
}

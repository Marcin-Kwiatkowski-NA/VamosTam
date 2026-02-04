package com.blablatwo.ride;

import com.blablatwo.city.City;
import com.blablatwo.city.CityDto;
import com.blablatwo.city.CityMapper;
import com.blablatwo.city.CityResolutionService;
import com.blablatwo.exceptions.AlreadyBookedException;
import com.blablatwo.exceptions.BookingNotFoundException;
import com.blablatwo.exceptions.NoSuchRideException;
import com.blablatwo.exceptions.RideFullException;
import com.blablatwo.exceptions.RideNotBookableException;
import com.blablatwo.ride.dto.ContactMethodDto;
import com.blablatwo.ride.dto.ContactType;
import com.blablatwo.ride.dto.DriverDto;
import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.ride.dto.RideSearchCriteriaDto;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import com.blablatwo.user.capability.CapabilityService;
import com.blablatwo.user.exception.NoSuchUserException;
import com.blablatwo.vehicle.Vehicle;
import com.blablatwo.vehicle.VehicleResponseDto;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.blablatwo.util.Constants.*;
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
        // Configure enricher to return input unchanged (pass-through for INTERNAL rides)
        lenient().when(rideResponseEnricher.enrich(any(Ride.class), any(RideResponseDto.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(rideResponseEnricher.enrich(anyList(), anyList()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        // Initialize CityDto objects for clarity (now using placeId instead of osmId)
        originCityDto = new CityDto(ID_ONE, CITY_NAME_ORIGIN, "PL", 100000L);
        destinationCityDto = new CityDto(2L, CITY_NAME_DESTINATION, "PL", 200000L);

        // Initialize City entities with new schema
        originCityEntity = City.builder()
                .id(ID_ONE)
                .placeId(ID_ONE)
                .namePl(CITY_NAME_ORIGIN)
                .normNamePl(CITY_NAME_ORIGIN.toLowerCase())
                .countryCode("PL")
                .population(100000L)
                .build();
        destinationCityEntity = City.builder()
                .id(2L)
                .placeId(2L)
                .namePl(CITY_NAME_DESTINATION)
                .normNamePl(CITY_NAME_DESTINATION.toLowerCase())
                .countryCode("PL")
                .population(200000L)
                .build();

        // Initialize the Ride entity
        ride = Ride.builder()
                .id(ID_100)
                .driver(UserAccount.builder().id(ID_ONE).email(TRAVELER_EMAIL_USER1).build())
                .origin(originCityEntity)
                .destination(destinationCityEntity)
                .departureTime(LOCAL_DATE_TIME)
                .availableSeats(ONE)
                .pricePerSeat(BIG_DECIMAL)
                .vehicle(Vehicle.builder().id(ID_ONE).licensePlate(VEHICLE_LICENSE_PLATE_1).build())
                .rideStatus(RideStatus.OPEN)
                .lastModified(INSTANT)
                .passengers(Collections.emptyList())
                .description(RIDE_DESCRIPTION)
                .build();

        // Initialize RideCreationDto with placeId fields (no driverId - comes from principal)
        rideCreationDTO = new RideCreationDto(
                ID_ONE,          // originPlaceId
                2L,              // destinationPlaceId
                LOCAL_DATE_TIME,
                false, // isApproximate
                ONE,
                BIG_DECIMAL,
                ID_ONE,
                RIDE_DESCRIPTION
        );

        // Initialize RideResponseDto with new structure
        rideResponseDto = RideResponseDto.builder()
                .id(ID_100)
                .source(RideSource.INTERNAL)
                .origin(originCityDto)
                .destination(destinationCityDto)
                .departureTime(LOCAL_DATE_TIME)
                .isApproximate(false)
                .pricePerSeat(BIG_DECIMAL)
                .availableSeats(ONE)
                .seatsTaken(0)
                .description(RIDE_DESCRIPTION)
                .driver(new DriverDto(ID_ONE, CRISTIANO, null, null))
                .contactMethods(List.of(new ContactMethodDto(ContactType.PHONE, TELEPHONE)))
                .vehicle(new VehicleResponseDto(ID_ONE, VEHICLE_MAKE_TESLA, VEHICLE_MODEL_MODEL_S, VEHICLE_PRODUCTION_YEAR_2021, VEHICLE_COLOR_RED, VEHICLE_LICENSE_PLATE_1))
                .rideStatus(RideStatus.OPEN)
                .build();
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

        @BeforeEach
        void setUpBooking() {
            passenger = UserAccount.builder()
                    .id(2L)
                    .email(TRAVELER_EMAIL_USER2)
                    .build();

            bookableRide = Ride.builder()
                    .id(ID_100)
                    .driver(UserAccount.builder().id(ID_ONE).email(TRAVELER_EMAIL_USER1).build())
                    .origin(originCityEntity)
                    .destination(destinationCityEntity)
                    .departureTime(LOCAL_DATE_TIME)
                    .availableSeats(3)
                    .pricePerSeat(BIG_DECIMAL)
                    .rideStatus(RideStatus.OPEN)
                    .passengers(new ArrayList<>())
                    .build();
        }

        @Test
        @DisplayName("Book ride successfully decreases available seats")
        void bookRide_Success() {
            // Arrange
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookableRide));
            when(capabilityService.canBook(2L)).thenReturn(true);
            when(rideRepository.existsPassenger(ID_100, 2L)).thenReturn(false);
            when(userAccountRepository.findById(2L)).thenReturn(Optional.of(passenger));
            when(rideRepository.save(any(Ride.class))).thenReturn(bookableRide);
            when(rideMapper.rideEntityToRideResponseDto(any())).thenReturn(rideResponseDto);

            // Act
            RideResponseDto result = rideService.bookRide(ID_100, 2L);

            // Assert
            assertNotNull(result);
            assertEquals(2, bookableRide.getAvailableSeats());
            assertTrue(bookableRide.getPassengers().contains(passenger));
            verify(rideRepository).save(bookableRide);
        }

        @Test
        @DisplayName("Book ride sets status to FULL when last seat is taken")
        void bookRide_SetsStatusToFull_WhenLastSeat() {
            // Arrange
            bookableRide.setAvailableSeats(1);
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookableRide));
            when(capabilityService.canBook(2L)).thenReturn(true);
            when(rideRepository.existsPassenger(ID_100, 2L)).thenReturn(false);
            when(userAccountRepository.findById(2L)).thenReturn(Optional.of(passenger));
            when(rideRepository.save(any(Ride.class))).thenReturn(bookableRide);
            when(rideMapper.rideEntityToRideResponseDto(any())).thenReturn(rideResponseDto);

            // Act
            rideService.bookRide(ID_100, 2L);

            // Assert
            assertEquals(0, bookableRide.getAvailableSeats());
            assertEquals(RideStatus.FULL, bookableRide.getRideStatus());
        }

        @Test
        @DisplayName("Book ride throws exception when ride is full")
        void bookRide_ThrowsWhenFull() {
            // Arrange
            bookableRide.setAvailableSeats(0);
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookableRide));
            when(capabilityService.canBook(2L)).thenReturn(true);
            when(rideRepository.existsPassenger(ID_100, 2L)).thenReturn(false);
            when(userAccountRepository.findById(2L)).thenReturn(Optional.of(passenger));

            // Act & Assert
            assertThrows(RideFullException.class, () -> rideService.bookRide(ID_100, 2L));
        }

        @Test
        @DisplayName("Book ride throws exception when already booked")
        void bookRide_ThrowsWhenAlreadyBooked() {
            // Arrange
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookableRide));
            when(capabilityService.canBook(2L)).thenReturn(true);
            when(rideRepository.existsPassenger(ID_100, 2L)).thenReturn(true);

            // Act & Assert
            assertThrows(AlreadyBookedException.class, () -> rideService.bookRide(ID_100, 2L));
        }

        @Test
        @DisplayName("Book ride throws exception when ride is not OPEN")
        void bookRide_ThrowsWhenNotOpen() {
            // Arrange
            bookableRide.setRideStatus(RideStatus.CANCELLED);
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookableRide));
            when(capabilityService.canBook(2L)).thenReturn(true);
            when(rideRepository.existsPassenger(ID_100, 2L)).thenReturn(false);
            when(userAccountRepository.findById(2L)).thenReturn(Optional.of(passenger));

            // Act & Assert
            assertThrows(RideNotBookableException.class, () -> rideService.bookRide(ID_100, 2L));
        }

        @Test
        @DisplayName("Book ride throws exception when ride not found")
        void bookRide_ThrowsWhenRideNotFound() {
            // Arrange
            when(rideRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(NoSuchRideException.class, () -> rideService.bookRide(NON_EXISTENT_ID, 2L));
        }

        @Test
        @DisplayName("Book ride throws exception when user not found")
        void bookRide_ThrowsWhenUserNotFound() {
            // Arrange
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookableRide));
            when(capabilityService.canBook(NON_EXISTENT_ID)).thenReturn(true);
            when(rideRepository.existsPassenger(ID_100, NON_EXISTENT_ID)).thenReturn(false);
            when(userAccountRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(NoSuchUserException.class, () -> rideService.bookRide(ID_100, NON_EXISTENT_ID));
        }
    }

    @Nested
    @DisplayName("Cancel Booking Tests")
    class CancelBookingTests {

        private UserAccount passenger;
        private Ride bookedRide;

        @BeforeEach
        void setUpCancellation() {
            passenger = UserAccount.builder()
                    .id(2L)
                    .email(TRAVELER_EMAIL_USER2)
                    .build();

            bookedRide = Ride.builder()
                    .id(ID_100)
                    .driver(UserAccount.builder().id(ID_ONE).email(TRAVELER_EMAIL_USER1).build())
                    .origin(originCityEntity)
                    .destination(destinationCityEntity)
                    .departureTime(LOCAL_DATE_TIME)
                    .availableSeats(2)
                    .pricePerSeat(BIG_DECIMAL)
                    .rideStatus(RideStatus.OPEN)
                    .passengers(new ArrayList<>(List.of(passenger)))
                    .build();
        }

        @Test
        @DisplayName("Cancel booking successfully increases available seats")
        void cancelBooking_Success() {
            // Arrange
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookedRide));
            when(userAccountRepository.existsById(2L)).thenReturn(true);
            when(rideRepository.save(any(Ride.class))).thenReturn(bookedRide);
            when(rideMapper.rideEntityToRideResponseDto(any())).thenReturn(rideResponseDto);

            // Act
            RideResponseDto result = rideService.cancelBooking(ID_100, 2L);

            // Assert
            assertNotNull(result);
            assertEquals(3, bookedRide.getAvailableSeats());
            assertFalse(bookedRide.getPassengers().contains(passenger));
            verify(rideRepository).save(bookedRide);
        }

        @Test
        @DisplayName("Cancel booking sets status to OPEN when ride was FULL")
        void cancelBooking_SetsStatusToOpen_WhenWasFull() {
            // Arrange
            bookedRide.setRideStatus(RideStatus.FULL);
            bookedRide.setAvailableSeats(0);
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookedRide));
            when(userAccountRepository.existsById(2L)).thenReturn(true);
            when(rideRepository.save(any(Ride.class))).thenReturn(bookedRide);
            when(rideMapper.rideEntityToRideResponseDto(any())).thenReturn(rideResponseDto);

            // Act
            rideService.cancelBooking(ID_100, 2L);

            // Assert
            assertEquals(1, bookedRide.getAvailableSeats());
            assertEquals(RideStatus.OPEN, bookedRide.getRideStatus());
        }

        @Test
        @DisplayName("Cancel booking throws exception when booking not found")
        void cancelBooking_ThrowsWhenBookingNotFound() {
            // Arrange
            bookedRide.setPassengers(new ArrayList<>());
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookedRide));
            when(userAccountRepository.existsById(2L)).thenReturn(true);

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
            UserAccount passenger = UserAccount.builder().id(2L).email(TRAVELER_EMAIL_USER2).build();
            when(userAccountRepository.findById(2L)).thenReturn(Optional.of(passenger));
            when(rideRepository.findByPassengersContaining(passenger)).thenReturn(List.of(ride));
            when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);

            // Act
            List<RideResponseDto> result = rideService.getRidesForPassenger(2L);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            verify(rideRepository).findByPassengersContaining(passenger);
        }

        @Test
        @DisplayName("Get rides for passenger returns empty list when no bookings")
        void getRidesForPassenger_ReturnsEmptyList() {
            // Arrange
            UserAccount passenger = UserAccount.builder().id(2L).email(TRAVELER_EMAIL_USER2).build();
            when(userAccountRepository.findById(2L)).thenReturn(Optional.of(passenger));
            when(rideRepository.findByPassengersContaining(passenger)).thenReturn(Collections.emptyList());

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
            when(userAccountRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(NoSuchUserException.class, () -> rideService.getRidesForPassenger(NON_EXISTENT_ID));
        }
    }
}

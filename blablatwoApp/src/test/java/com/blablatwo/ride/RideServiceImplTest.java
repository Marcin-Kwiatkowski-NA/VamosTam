package com.blablatwo.ride;

import com.blablatwo.city.City;
import com.blablatwo.city.CityDto;
import com.blablatwo.city.CityMapper;
import com.blablatwo.city.CityRepository;
import com.blablatwo.exceptions.AlreadyBookedException;
import com.blablatwo.exceptions.BookingNotFoundException;
import com.blablatwo.exceptions.NoSuchRideException;
import com.blablatwo.exceptions.NoSuchTravelerException;
import com.blablatwo.exceptions.RideFullException;
import com.blablatwo.exceptions.RideNotBookableException;
import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.ride.dto.RideSearchCriteriaDto;
import com.blablatwo.traveler.DriverProfileDto;
import com.blablatwo.traveler.Traveler;
import com.blablatwo.traveler.TravelerRepository;
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

    @Mock
    TravelerRepository travelerRepository;

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

    @Nested
    @DisplayName("Search Rides Tests")
    class SearchRidesTests {

        @Test
        @DisplayName("Search rides returns paginated results")
        void searchRides_ReturnsPagedResults() {
            // Arrange
            RideSearchCriteriaDto criteria = new RideSearchCriteriaDto(
                    CITY_NAME_ORIGIN, CITY_NAME_DESTINATION, LocalDate.now(), null, 1
            );
            Pageable pageable = PageRequest.of(0, 10);
            Page<Ride> ridePage = new PageImpl<>(List.of(ride));

            when(rideRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(ridePage);
            when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);

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
            RideSearchCriteriaDto criteria = new RideSearchCriteriaDto(null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Ride> ridePage = new PageImpl<>(List.of(ride));

            when(rideRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(ridePage);
            when(rideMapper.rideEntityToRideResponseDto(ride)).thenReturn(rideResponseDto);

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

        private Traveler passenger;
        private Ride bookableRide;

        @BeforeEach
        void setUpBooking() {
            passenger = Traveler.builder()
                    .id(2L)
                    .username(TRAVELER_USERNAME_USER2)
                    .email(TRAVELER_EMAIL_USER2)
                    .build();

            bookableRide = Ride.builder()
                    .id(ID_100)
                    .driver(Traveler.builder().id(ID_ONE).username(TRAVELER_USERNAME_USER1).build())
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
            when(travelerRepository.findById(2L)).thenReturn(Optional.of(passenger));
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
            when(travelerRepository.findById(2L)).thenReturn(Optional.of(passenger));
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
            when(travelerRepository.findById(2L)).thenReturn(Optional.of(passenger));

            // Act & Assert
            assertThrows(RideFullException.class, () -> rideService.bookRide(ID_100, 2L));
        }

        @Test
        @DisplayName("Book ride throws exception when already booked")
        void bookRide_ThrowsWhenAlreadyBooked() {
            // Arrange
            bookableRide.getPassengers().add(passenger);
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookableRide));
            when(travelerRepository.findById(2L)).thenReturn(Optional.of(passenger));

            // Act & Assert
            assertThrows(AlreadyBookedException.class, () -> rideService.bookRide(ID_100, 2L));
        }

        @Test
        @DisplayName("Book ride throws exception when ride is not OPEN")
        void bookRide_ThrowsWhenNotOpen() {
            // Arrange
            bookableRide.setRideStatus(RideStatus.CANCELLED);
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookableRide));
            when(travelerRepository.findById(2L)).thenReturn(Optional.of(passenger));

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
        @DisplayName("Book ride throws exception when traveler not found")
        void bookRide_ThrowsWhenTravelerNotFound() {
            // Arrange
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(bookableRide));
            when(travelerRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(NoSuchTravelerException.class, () -> rideService.bookRide(ID_100, NON_EXISTENT_ID));
        }
    }

    @Nested
    @DisplayName("Cancel Booking Tests")
    class CancelBookingTests {

        private Traveler passenger;
        private Ride bookedRide;

        @BeforeEach
        void setUpCancellation() {
            passenger = Traveler.builder()
                    .id(2L)
                    .username(TRAVELER_USERNAME_USER2)
                    .email(TRAVELER_EMAIL_USER2)
                    .build();

            bookedRide = Ride.builder()
                    .id(ID_100)
                    .driver(Traveler.builder().id(ID_ONE).username(TRAVELER_USERNAME_USER1).build())
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
            when(travelerRepository.findById(2L)).thenReturn(Optional.of(passenger));
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
            when(travelerRepository.findById(2L)).thenReturn(Optional.of(passenger));
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
            when(travelerRepository.findById(2L)).thenReturn(Optional.of(passenger));

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
            Traveler passenger = Traveler.builder().id(2L).username(TRAVELER_USERNAME_USER2).build();
            when(travelerRepository.findById(2L)).thenReturn(Optional.of(passenger));
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
            Traveler passenger = Traveler.builder().id(2L).username(TRAVELER_USERNAME_USER2).build();
            when(travelerRepository.findById(2L)).thenReturn(Optional.of(passenger));
            when(rideRepository.findByPassengersContaining(passenger)).thenReturn(Collections.emptyList());

            // Act
            List<RideResponseDto> result = rideService.getRidesForPassenger(2L);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Get rides for passenger throws exception when traveler not found")
        void getRidesForPassenger_ThrowsWhenTravelerNotFound() {
            // Arrange
            when(travelerRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(NoSuchTravelerException.class, () -> rideService.getRidesForPassenger(NON_EXISTENT_ID));
        }
    }
}
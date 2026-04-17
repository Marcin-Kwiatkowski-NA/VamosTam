package com.vamigo.ride;

import com.vamigo.domain.Status;
import com.vamigo.exceptions.AlreadyBookedException;
import com.vamigo.exceptions.CannotBookOwnRideException;
import com.vamigo.exceptions.ExternalRideNotBookableException;
import com.vamigo.exceptions.InsufficientSeatsException;
import com.vamigo.exceptions.InvalidBookingTransitionException;
import com.vamigo.exceptions.NoSuchRideException;
import com.vamigo.exceptions.NotRideDriverException;
import com.vamigo.exceptions.RideNotBookableException;
import com.vamigo.location.Location;
import com.vamigo.ride.dto.BookRideRequest;
import com.vamigo.ride.dto.BookingResponseDto;
import com.vamigo.ride.event.BookingCancelledEvent;
import com.vamigo.ride.event.BookingConfirmedEvent;
import com.vamigo.ride.event.BookingRequestedEvent;
import com.vamigo.ride.event.BookingRejectedEvent;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import com.vamigo.user.UserProfileRepository;
import com.vamigo.user.capability.CapabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static com.vamigo.util.Constants.*;
import static com.vamigo.util.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock private RideRepository rideRepository;
    @Mock private RideBookingRepository bookingRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private BookingMapper bookingMapper;
    @Mock private BookingResponseEnricher bookingResponseEnricher;
    @Mock private CapabilityService capabilityService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private Ride ride;
    private UserAccount passenger;
    private Location origin;
    private Location destination;

    @BeforeEach
    void setUp() {
        origin = anOriginLocation().build();
        destination = aDestinationLocation().build();
        ride = buildRideWithStops(origin, destination);
        ride.setSource(RideSource.INTERNAL);
        ride.setStatus(Status.ACTIVE);
        ride.setTotalSeats(3);
        ride.setAutoApprove(true);
        passenger = aPassengerAccount().build();
        lenient().when(userProfileRepository.findById(any())).thenReturn(Optional.empty());
    }

    private void stubEnricher() {
        BookingResponseDto dto = aBookingResponseDto().build();
        lenient().when(bookingMapper.toResponseDto(any())).thenReturn(dto);
        lenient().when(bookingResponseEnricher.enrich(any(RideBooking.class), any()))
                .thenReturn(dto);
    }

    @Nested
    @DisplayName("Create a booking for a ride")
    class CreateBookingTests {

        @Test
        @DisplayName("Creates a CONFIRMED booking when the ride has auto-approve enabled")
        void autoApproveCreatesConfirmedBooking() {
            ride.setAutoApprove(true);
            when(rideRepository.findByIdForUpdate(ID_100)).thenReturn(Optional.of(ride));
            when(capabilityService.canBook(2L)).thenReturn(true);
            when(bookingRepository.existsByRideIdAndPassengerIdAndStatusIn(eq(ID_100), eq(2L), any()))
                    .thenReturn(false);
            when(userAccountRepository.findById(2L)).thenReturn(Optional.of(passenger));
            stubEnricher();

            bookingService.createBooking(ID_100, 2L, aBookRideRequest().build());

            ArgumentCaptor<RideBooking> captor = ArgumentCaptor.forClass(RideBooking.class);
            verify(bookingRepository).save(captor.capture());
            assertEquals(BookingStatus.CONFIRMED, captor.getValue().getStatus());
            assertNotNull(captor.getValue().getResolvedAt());
            verify(eventPublisher).publishEvent(any(BookingConfirmedEvent.class));
        }

        @Test
        @DisplayName("Creates a PENDING booking when the ride requires driver approval")
        void manualApproveCreatesPendingBooking() {
            ride.setAutoApprove(false);
            when(rideRepository.findByIdForUpdate(ID_100)).thenReturn(Optional.of(ride));
            when(capabilityService.canBook(2L)).thenReturn(true);
            when(bookingRepository.existsByRideIdAndPassengerIdAndStatusIn(eq(ID_100), eq(2L), any()))
                    .thenReturn(false);
            when(userAccountRepository.findById(2L)).thenReturn(Optional.of(passenger));
            stubEnricher();

            bookingService.createBooking(ID_100, 2L, aBookRideRequest().build());

            ArgumentCaptor<RideBooking> captor = ArgumentCaptor.forClass(RideBooking.class);
            verify(bookingRepository).save(captor.capture());
            assertEquals(BookingStatus.PENDING, captor.getValue().getStatus());
            assertNull(captor.getValue().getResolvedAt());
            verify(eventPublisher).publishEvent(any(BookingRequestedEvent.class));
        }

        @Test
        @DisplayName("Stores the requested seat count when more than one seat is booked")
        void multiSeatBooking() {
            when(rideRepository.findByIdForUpdate(ID_100)).thenReturn(Optional.of(ride));
            when(capabilityService.canBook(2L)).thenReturn(true);
            when(bookingRepository.existsByRideIdAndPassengerIdAndStatusIn(eq(ID_100), eq(2L), any()))
                    .thenReturn(false);
            when(userAccountRepository.findById(2L)).thenReturn(Optional.of(passenger));
            stubEnricher();

            BookRideRequest request = aBookRideRequest().seatCount(2).build();
            bookingService.createBooking(ID_100, 2L, request);

            ArgumentCaptor<RideBooking> captor = ArgumentCaptor.forClass(RideBooking.class);
            verify(bookingRepository).save(captor.capture());
            assertEquals(2, captor.getValue().getSeatCount());
        }

        @Test
        @DisplayName("Throws InsufficientSeatsException when the ride has fewer seats than requested")
        void throwsWhenInsufficientSeats() {
            ride.setTotalSeats(1);
            when(rideRepository.findByIdForUpdate(ID_100)).thenReturn(Optional.of(ride));
            when(capabilityService.canBook(2L)).thenReturn(true);
            when(bookingRepository.existsByRideIdAndPassengerIdAndStatusIn(eq(ID_100), eq(2L), any()))
                    .thenReturn(false);
            when(userAccountRepository.findById(2L)).thenReturn(Optional.of(passenger));

            BookRideRequest request = aBookRideRequest().seatCount(2).build();
            assertThrows(InsufficientSeatsException.class,
                    () -> bookingService.createBooking(ID_100, 2L, request));
        }

        @Test
        @DisplayName("Throws AlreadyBookedException when the passenger already has an active booking on the ride")
        void throwsOnDuplicateBooking() {
            when(rideRepository.findByIdForUpdate(ID_100)).thenReturn(Optional.of(ride));
            when(capabilityService.canBook(2L)).thenReturn(true);
            when(bookingRepository.existsByRideIdAndPassengerIdAndStatusIn(eq(ID_100), eq(2L), any()))
                    .thenReturn(true);

            assertThrows(AlreadyBookedException.class,
                    () -> bookingService.createBooking(ID_100, 2L, aBookRideRequest().build()));
        }

        @Test
        @DisplayName("Throws NoSuchRideException when the ride id does not exist")
        void throwsForMissingRide() {
            when(rideRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

            assertThrows(NoSuchRideException.class,
                    () -> bookingService.createBooking(999L, 2L, aBookRideRequest().build()));
        }

        @Test
        @DisplayName("Throws CannotBookOwnRideException when the driver tries to book their own ride")
        void throwsWhenDriverBooksOwnRide() {
            when(rideRepository.findByIdForUpdate(ID_100)).thenReturn(Optional.of(ride));

            assertThrows(CannotBookOwnRideException.class,
                    () -> bookingService.createBooking(ID_100, ID_ONE, aBookRideRequest().build()));
        }

        @Test
        @DisplayName("Throws ExternalRideNotBookableException when the ride was imported from Facebook")
        void throwsForExternalRide() {
            ride.setSource(RideSource.FACEBOOK);
            when(rideRepository.findByIdForUpdate(ID_100)).thenReturn(Optional.of(ride));

            assertThrows(ExternalRideNotBookableException.class,
                    () -> bookingService.createBooking(ID_100, 2L, aBookRideRequest().build()));
        }

        @Test
        @DisplayName("Throws RideNotBookableException when the ride has been cancelled")
        void throwsForCancelledRide() {
            ride.setStatus(Status.CANCELLED);
            when(rideRepository.findByIdForUpdate(ID_100)).thenReturn(Optional.of(ride));
            when(capabilityService.canBook(2L)).thenReturn(true);
            when(bookingRepository.existsByRideIdAndPassengerIdAndStatusIn(eq(ID_100), eq(2L), any()))
                    .thenReturn(false);
            when(userAccountRepository.findById(2L)).thenReturn(Optional.of(passenger));

            assertThrows(RideNotBookableException.class,
                    () -> bookingService.createBooking(ID_100, 2L, aBookRideRequest().build()));
        }
    }

    @Nested
    @DisplayName("Driver confirms a booking")
    class ConfirmBookingTests {

        @Test
        @DisplayName("Moves a PENDING booking to CONFIRMED and publishes BookingConfirmedEvent")
        void confirmsPendingBooking() {
            RideBooking booking = aBooking(ride, passenger)
                    .status(BookingStatus.PENDING).resolvedAt(null).build();
            when(rideRepository.findByIdForUpdate(ID_100)).thenReturn(Optional.of(ride));
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
            stubEnricher();

            bookingService.confirmBooking(ID_100, 1L, ID_ONE);

            assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
            assertNotNull(booking.getResolvedAt());
            verify(eventPublisher).publishEvent(any(BookingConfirmedEvent.class));
        }

        @Test
        @DisplayName("Throws NotRideDriverException when the caller is not the ride's driver")
        void throwsForNonDriver() {
            when(rideRepository.findByIdForUpdate(ID_100)).thenReturn(Optional.of(ride));

            assertThrows(NotRideDriverException.class,
                    () -> bookingService.confirmBooking(ID_100, 1L, 999L));
        }

        @Test
        @DisplayName("Throws InvalidBookingTransitionException when the booking is already CONFIRMED")
        void throwsForAlreadyConfirmed() {
            RideBooking booking = aBooking(ride, passenger)
                    .status(BookingStatus.CONFIRMED).build();
            when(rideRepository.findByIdForUpdate(ID_100)).thenReturn(Optional.of(ride));
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            assertThrows(InvalidBookingTransitionException.class,
                    () -> bookingService.confirmBooking(ID_100, 1L, ID_ONE));
        }
    }

    @Nested
    @DisplayName("Driver rejects a booking")
    class RejectBookingTests {

        @Test
        @DisplayName("Moves a PENDING booking to REJECTED and publishes BookingRejectedEvent")
        void rejectsPendingBooking() {
            RideBooking booking = aBooking(ride, passenger)
                    .status(BookingStatus.PENDING).resolvedAt(null).build();
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(ride));
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
            stubEnricher();

            bookingService.rejectBooking(ID_100, 1L, ID_ONE);

            assertEquals(BookingStatus.REJECTED, booking.getStatus());
            assertNotNull(booking.getResolvedAt());
            verify(eventPublisher).publishEvent(any(BookingRejectedEvent.class));
        }

        @Test
        @DisplayName("Throws InvalidBookingTransitionException when rejecting a CONFIRMED booking")
        void throwsForConfirmedBooking() {
            RideBooking booking = aBooking(ride, passenger)
                    .status(BookingStatus.CONFIRMED).build();
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(ride));
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            assertThrows(InvalidBookingTransitionException.class,
                    () -> bookingService.rejectBooking(ID_100, 1L, ID_ONE));
        }
    }

    @Nested
    @DisplayName("Cancel an existing booking")
    class CancelBookingTests {

        @Test
        @DisplayName("Passenger cancellation on a CONFIRMED booking sets status to CANCELLED_BY_PASSENGER")
        void passengerCancelsConfirmedBooking() {
            RideBooking booking = aBooking(ride, passenger)
                    .status(BookingStatus.CONFIRMED).build();
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(ride));
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
            stubEnricher();

            bookingService.cancelBooking(ID_100, 1L, 2L, "No longer need this ride");

            assertEquals(BookingStatus.CANCELLED_BY_PASSENGER, booking.getStatus());
            verify(eventPublisher).publishEvent(any(BookingCancelledEvent.class));
        }

        @Test
        @DisplayName("Driver cancellation on a CONFIRMED booking sets status to CANCELLED_BY_DRIVER")
        void driverCancelsConfirmedBooking() {
            RideBooking booking = aBooking(ride, passenger)
                    .status(BookingStatus.CONFIRMED).build();
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(ride));
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
            stubEnricher();

            bookingService.cancelBooking(ID_100, 1L, ID_ONE, "Cancelling this booking");

            assertEquals(BookingStatus.CANCELLED_BY_DRIVER, booking.getStatus());
        }

        @Test
        @DisplayName("Allows passenger to cancel a PENDING booking without supplying a reason")
        void passengerCancelsPendingBookingWithoutReason() {
            RideBooking booking = aBooking(ride, passenger)
                    .status(BookingStatus.PENDING).resolvedAt(null).build();
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(ride));
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
            stubEnricher();

            bookingService.cancelBooking(ID_100, 1L, 2L, null);

            assertEquals(BookingStatus.CANCELLED_BY_PASSENGER, booking.getStatus());
            assertNull(booking.getCancellationReason());
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when a CONFIRMED booking is cancelled without a reason")
        void throwsWhenCancellingConfirmedWithoutReason() {
            RideBooking booking = aBooking(ride, passenger)
                    .status(BookingStatus.CONFIRMED).build();
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(ride));
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            assertThrows(IllegalArgumentException.class,
                    () -> bookingService.cancelBooking(ID_100, 1L, 2L, null));
        }

        @Test
        @DisplayName("Throws InvalidBookingTransitionException when cancelling an already REJECTED booking")
        void throwsForRejectedBooking() {
            RideBooking booking = aBooking(ride, passenger)
                    .status(BookingStatus.REJECTED).build();
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(ride));
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            assertThrows(InvalidBookingTransitionException.class,
                    () -> bookingService.cancelBooking(ID_100, 1L, 2L, "Some reason"));
        }

        @Test
        @DisplayName("Throws NotRideDriverException when a user unrelated to the booking attempts to cancel it")
        void throwsForUnrelatedUser() {
            RideBooking booking = aBooking(ride, passenger)
                    .status(BookingStatus.CONFIRMED).build();
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(ride));
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            assertThrows(NotRideDriverException.class,
                    () -> bookingService.cancelBooking(ID_100, 1L, 999L, "Some reason"));
        }
    }

    @Nested
    @DisplayName("List bookings for a ride")
    class GetBookingsForRideTests {

        @Test
        @DisplayName("Returns the ride's bookings when the caller is the driver")
        void returnsBookingsForDriver() {
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(ride));
            when(bookingRepository.findByRideId(ID_100)).thenReturn(List.of());
            when(bookingMapper.toResponseDtos(any())).thenReturn(List.of());
            when(bookingResponseEnricher.enrich(anyList(), anyList())).thenReturn(List.of());

            List<BookingResponseDto> result = bookingService.getBookingsForRide(ID_100, ID_ONE);
            assertNotNull(result);
        }

        @Test
        @DisplayName("Throws NotRideDriverException when a non-driver requests the booking list")
        void throwsForNonDriver() {
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(ride));

            assertThrows(NotRideDriverException.class,
                    () -> bookingService.getBookingsForRide(ID_100, 999L));
        }
    }
}

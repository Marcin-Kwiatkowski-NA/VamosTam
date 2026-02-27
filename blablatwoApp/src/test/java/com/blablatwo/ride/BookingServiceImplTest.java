package com.blablatwo.ride;

import com.blablatwo.domain.Status;
import com.blablatwo.exceptions.AlreadyBookedException;
import com.blablatwo.exceptions.CannotBookOwnRideException;
import com.blablatwo.exceptions.ExternalRideNotBookableException;
import com.blablatwo.exceptions.InsufficientSeatsException;
import com.blablatwo.exceptions.InvalidBookingTransitionException;
import com.blablatwo.exceptions.NoSuchRideException;
import com.blablatwo.exceptions.NotRideDriverException;
import com.blablatwo.exceptions.RideNotBookableException;
import com.blablatwo.location.Location;
import com.blablatwo.ride.dto.BookRideRequest;
import com.blablatwo.ride.dto.BookingResponseDto;
import com.blablatwo.ride.event.BookingCancelledEvent;
import com.blablatwo.ride.event.BookingConfirmedEvent;
import com.blablatwo.ride.event.BookingRequestedEvent;
import com.blablatwo.ride.event.BookingRejectedEvent;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import com.blablatwo.user.capability.CapabilityService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.blablatwo.util.Constants.*;
import static com.blablatwo.util.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock private RideRepository rideRepository;
    @Mock private RideBookingRepository bookingRepository;
    @Mock private UserAccountRepository userAccountRepository;
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
    }

    private void stubEnricher() {
        BookingResponseDto dto = aBookingResponseDto().build();
        lenient().when(bookingMapper.toResponseDto(any())).thenReturn(dto);
        lenient().when(bookingResponseEnricher.enrich(any(RideBooking.class), any()))
                .thenReturn(dto);
    }

    @Nested
    @DisplayName("createBooking")
    class CreateBookingTests {

        @Test
        @DisplayName("auto-approve ride creates CONFIRMED booking")
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
        @DisplayName("manual-approve ride creates PENDING booking")
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
        @DisplayName("multi-seat booking sets seatCount correctly")
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
        @DisplayName("throws InsufficientSeatsException when not enough seats")
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
        @DisplayName("throws AlreadyBookedException for duplicate active booking")
        void throwsOnDuplicateBooking() {
            when(rideRepository.findByIdForUpdate(ID_100)).thenReturn(Optional.of(ride));
            when(capabilityService.canBook(2L)).thenReturn(true);
            when(bookingRepository.existsByRideIdAndPassengerIdAndStatusIn(eq(ID_100), eq(2L), any()))
                    .thenReturn(true);

            assertThrows(AlreadyBookedException.class,
                    () -> bookingService.createBooking(ID_100, 2L, aBookRideRequest().build()));
        }

        @Test
        @DisplayName("throws NoSuchRideException for non-existent ride")
        void throwsForMissingRide() {
            when(rideRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

            assertThrows(NoSuchRideException.class,
                    () -> bookingService.createBooking(999L, 2L, aBookRideRequest().build()));
        }

        @Test
        @DisplayName("throws CannotBookOwnRideException when driver books own ride")
        void throwsWhenDriverBooksOwnRide() {
            when(rideRepository.findByIdForUpdate(ID_100)).thenReturn(Optional.of(ride));

            assertThrows(CannotBookOwnRideException.class,
                    () -> bookingService.createBooking(ID_100, ID_ONE, aBookRideRequest().build()));
        }

        @Test
        @DisplayName("throws ExternalRideNotBookableException for FACEBOOK ride")
        void throwsForExternalRide() {
            ride.setSource(RideSource.FACEBOOK);
            when(rideRepository.findByIdForUpdate(ID_100)).thenReturn(Optional.of(ride));

            assertThrows(ExternalRideNotBookableException.class,
                    () -> bookingService.createBooking(ID_100, 2L, aBookRideRequest().build()));
        }

        @Test
        @DisplayName("throws RideNotBookableException for CANCELLED ride")
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
    @DisplayName("confirmBooking")
    class ConfirmBookingTests {

        @Test
        @DisplayName("confirms PENDING booking")
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
        @DisplayName("throws NotRideDriverException for non-driver")
        void throwsForNonDriver() {
            when(rideRepository.findByIdForUpdate(ID_100)).thenReturn(Optional.of(ride));

            assertThrows(NotRideDriverException.class,
                    () -> bookingService.confirmBooking(ID_100, 1L, 999L));
        }

        @Test
        @DisplayName("throws InvalidBookingTransitionException for CONFIRMED booking")
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
    @DisplayName("rejectBooking")
    class RejectBookingTests {

        @Test
        @DisplayName("rejects PENDING booking")
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
        @DisplayName("throws InvalidBookingTransitionException for CONFIRMED booking")
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
    @DisplayName("cancelBooking")
    class CancelBookingTests {

        @Test
        @DisplayName("passenger cancels CONFIRMED booking")
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
        @DisplayName("driver cancels CONFIRMED booking")
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
        @DisplayName("passenger cancels PENDING booking without reason")
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
        @DisplayName("throws IllegalArgumentException when cancelling CONFIRMED booking without reason")
        void throwsWhenCancellingConfirmedWithoutReason() {
            RideBooking booking = aBooking(ride, passenger)
                    .status(BookingStatus.CONFIRMED).build();
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(ride));
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            assertThrows(IllegalArgumentException.class,
                    () -> bookingService.cancelBooking(ID_100, 1L, 2L, null));
        }

        @Test
        @DisplayName("throws InvalidBookingTransitionException for REJECTED booking")
        void throwsForRejectedBooking() {
            RideBooking booking = aBooking(ride, passenger)
                    .status(BookingStatus.REJECTED).build();
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(ride));
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            assertThrows(InvalidBookingTransitionException.class,
                    () -> bookingService.cancelBooking(ID_100, 1L, 2L, "Some reason"));
        }

        @Test
        @DisplayName("throws NotRideDriverException for unrelated user")
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
    @DisplayName("getBookingsForRide")
    class GetBookingsForRideTests {

        @Test
        @DisplayName("returns bookings for driver")
        void returnsBookingsForDriver() {
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(ride));
            when(bookingRepository.findByRideId(ID_100)).thenReturn(List.of());
            when(bookingMapper.toResponseDtos(any())).thenReturn(List.of());
            when(bookingResponseEnricher.enrich(anyList(), anyList())).thenReturn(List.of());

            List<BookingResponseDto> result = bookingService.getBookingsForRide(ID_100, ID_ONE);
            assertNotNull(result);
        }

        @Test
        @DisplayName("throws NotRideDriverException for non-driver")
        void throwsForNonDriver() {
            when(rideRepository.findById(ID_100)).thenReturn(Optional.of(ride));

            assertThrows(NotRideDriverException.class,
                    () -> bookingService.getBookingsForRide(ID_100, 999L));
        }
    }
}

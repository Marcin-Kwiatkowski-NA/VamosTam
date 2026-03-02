package com.vamigo.ride;

import com.vamigo.ride.event.BookingExpiredEvent;
import com.vamigo.user.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.vamigo.util.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingExpirySchedulerTest {

    @Mock
    private RideBookingRepository bookingRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BookingExpiryScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "pendingTtlMinutes", 60);
    }

    @Test
    @DisplayName("Expires stale PENDING bookings and publishes events")
    void expiresPendingBookings() {
        Ride ride = buildRideWithStops();
        UserAccount passenger = aPassengerAccount().build();

        RideBooking staleBooking = aBooking(ride, passenger)
                .id(10L)
                .status(BookingStatus.PENDING)
                .resolvedAt(null)
                .bookedAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .build();

        when(bookingRepository.findByStatusAndBookedAtBefore(eq(BookingStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(staleBooking));

        scheduler.expirePendingBookings();

        assertEquals(BookingStatus.EXPIRED, staleBooking.getStatus());
        assertNotNull(staleBooking.getResolvedAt());

        ArgumentCaptor<BookingExpiredEvent> captor = ArgumentCaptor.forClass(BookingExpiredEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        BookingExpiredEvent event = captor.getValue();
        assertEquals(10L, event.bookingId());
        assertEquals(ride.getId(), event.rideId());
        assertEquals(passenger.getId(), event.passengerId());
        assertEquals(ride.getDriver().getId(), event.driverId());
    }

    @Test
    @DisplayName("Expires multiple stale bookings")
    void expiresMultipleBookings() {
        Ride ride = buildRideWithStops();
        UserAccount passenger1 = aPassengerAccount().build();
        UserAccount passenger2 = aPassengerAccount().id(3L).build();

        RideBooking booking1 = aBooking(ride, passenger1)
                .id(10L).status(BookingStatus.PENDING).resolvedAt(null)
                .bookedAt(Instant.now().minus(2, ChronoUnit.HOURS)).build();
        RideBooking booking2 = aBooking(ride, passenger2)
                .id(11L).status(BookingStatus.PENDING).resolvedAt(null)
                .bookedAt(Instant.now().minus(3, ChronoUnit.HOURS)).build();

        when(bookingRepository.findByStatusAndBookedAtBefore(eq(BookingStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(booking1, booking2));

        scheduler.expirePendingBookings();

        assertEquals(BookingStatus.EXPIRED, booking1.getStatus());
        assertEquals(BookingStatus.EXPIRED, booking2.getStatus());
        verify(eventPublisher, times(2)).publishEvent(any(BookingExpiredEvent.class));
    }

    @Test
    @DisplayName("Does nothing when no stale bookings exist")
    void noStaleBookings() {
        when(bookingRepository.findByStatusAndBookedAtBefore(eq(BookingStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of());

        scheduler.expirePendingBookings();

        verify(eventPublisher, never()).publishEvent(any());
    }
}

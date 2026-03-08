package com.vamigo.ride;

import com.vamigo.auth.AppPrincipal;
import com.vamigo.user.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingSecurityTest {

    @Mock
    private RideBookingRepository bookingRepository;

    @Mock
    private RideRepository rideRepository;

    @InjectMocks
    private BookingSecurity bookingSecurity;

    private final AppPrincipal principal = new AppPrincipal(1L, "user@test.com", Set.of(Role.USER));

    @Test
    @DisplayName("isRideDriver returns true when user is the ride driver")
    void isRideDriver_True() {
        when(rideRepository.existsByIdAndDriverId(100L, 1L)).thenReturn(true);

        assertTrue(bookingSecurity.isRideDriver(principal, 100L));
    }

    @Test
    @DisplayName("isRideDriver returns false when user is not the ride driver")
    void isRideDriver_False() {
        when(rideRepository.existsByIdAndDriverId(100L, 1L)).thenReturn(false);

        assertFalse(bookingSecurity.isRideDriver(principal, 100L));
    }

    @Test
    @DisplayName("isBookingParticipant returns true when user is participant")
    void isBookingParticipant_True() {
        when(bookingRepository.isUserParticipant(100L, 1L, 1L)).thenReturn(true);

        assertTrue(bookingSecurity.isBookingParticipant(principal, 100L, 1L));
    }

    @Test
    @DisplayName("isBookingParticipant returns false when user is not participant")
    void isBookingParticipant_False() {
        when(bookingRepository.isUserParticipant(100L, 1L, 1L)).thenReturn(false);

        assertFalse(bookingSecurity.isBookingParticipant(principal, 100L, 1L));
    }
}

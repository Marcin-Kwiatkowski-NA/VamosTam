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
class RideSecurityTest {

    @Mock
    private RideRepository rideRepository;

    @InjectMocks
    private RideSecurity rideSecurity;

    private final AppPrincipal principal = new AppPrincipal(1L, "driver@test.com", Set.of(Role.USER));

    @Test
    @DisplayName("isDriver returns true when user is the ride driver")
    void isDriver_True() {
        when(rideRepository.existsByIdAndDriverId(100L, 1L)).thenReturn(true);

        assertTrue(rideSecurity.isDriver(principal, 100L));
    }

    @Test
    @DisplayName("isDriver returns false when user is not the ride driver")
    void isDriver_False() {
        when(rideRepository.existsByIdAndDriverId(100L, 1L)).thenReturn(false);

        assertFalse(rideSecurity.isDriver(principal, 100L));
    }
}

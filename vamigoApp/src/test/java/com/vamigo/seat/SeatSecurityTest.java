package com.vamigo.seat;

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
class SeatSecurityTest {

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private SeatSecurity seatSecurity;

    private final AppPrincipal principal = new AppPrincipal(1L, "passenger@test.com", Set.of(Role.USER));

    @Test
    @DisplayName("isPassenger returns true when user is the seat passenger")
    void isPassenger_True() {
        when(seatRepository.existsByIdAndPassengerId(100L, 1L)).thenReturn(true);

        assertTrue(seatSecurity.isPassenger(principal, 100L));
    }

    @Test
    @DisplayName("isPassenger returns false when user is not the seat passenger")
    void isPassenger_False() {
        when(seatRepository.existsByIdAndPassengerId(100L, 1L)).thenReturn(false);

        assertFalse(seatSecurity.isPassenger(principal, 100L));
    }
}

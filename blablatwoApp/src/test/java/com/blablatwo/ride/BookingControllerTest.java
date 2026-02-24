package com.blablatwo.ride;

import com.blablatwo.auth.AppPrincipal;
import com.blablatwo.auth.service.JwtTokenProvider;
import com.blablatwo.exceptions.InsufficientSeatsException;
import com.blablatwo.exceptions.InvalidBookingTransitionException;
import com.blablatwo.exceptions.NoSuchRideException;
import com.blablatwo.exceptions.NotRideDriverException;
import com.blablatwo.ride.dto.BookRideRequest;
import com.blablatwo.ride.dto.BookingResponseDto;
import com.blablatwo.user.Role;
import com.blablatwo.user.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Set;

import static com.blablatwo.util.Constants.*;
import static com.blablatwo.util.TestFixtures.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Note: In Spring Boot 4.0 / Spring Security 7.0, {@code @AuthenticationPrincipal AppPrincipal}
 * resolves via model attribute data binding in @WebMvcTest, yielding null record fields.
 * Service mocks use {@code any()} for principal-derived userId arguments.
 */
@WebMvcTest(controllers = BookingController.class)
class BookingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;

    @MockitoBean private BookingService bookingService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserAccountRepository userAccountRepository;

    private BookingResponseDto bookingResponseDto;

    @BeforeEach
    void setUp() {
        bookingResponseDto = aBookingResponseDto().build();
    }

    private static UsernamePasswordAuthenticationToken userAuth() {
        AppPrincipal p = new AppPrincipal(2L, TRAVELER_EMAIL_USER2, Set.of(Role.USER));
        return new UsernamePasswordAuthenticationToken(p, null, p.roles());
    }

    @Nested
    @DisplayName("POST /rides/{rideId}/bookings")
    class CreateBookingTests {

        @Test
        @DisplayName("201 Created on success")
        void createBooking_Success() throws Exception {
            when(bookingService.createBooking(eq(ID_100), any(), any(BookRideRequest.class)))
                    .thenReturn(bookingResponseDto);

            mockMvc.perform(post("/rides/" + ID_100 + "/bookings")
                            .with(csrf())
                            .with(authentication(userAuth()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(aBookRideRequest().build())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("404 when ride not found")
        void createBooking_RideNotFound() throws Exception {
            when(bookingService.createBooking(eq(999L), any(), any(BookRideRequest.class)))
                    .thenThrow(new NoSuchRideException(999L));

            mockMvc.perform(post("/rides/999/bookings")
                            .with(csrf())
                            .with(authentication(userAuth()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(aBookRideRequest().build())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("409 when insufficient seats")
        void createBooking_InsufficientSeats() throws Exception {
            when(bookingService.createBooking(eq(ID_100), any(), any(BookRideRequest.class)))
                    .thenThrow(new InsufficientSeatsException(ID_100, 3, 1));

            mockMvc.perform(post("/rides/" + ID_100 + "/bookings")
                            .with(csrf())
                            .with(authentication(userAuth()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(aBookRideRequest().seatCount(3).build())))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /rides/{rideId}/bookings/{bookingId}/confirm")
    class ConfirmBookingTests {

        @Test
        @DisplayName("200 OK on success")
        void confirmBooking_Success() throws Exception {
            when(bookingService.confirmBooking(eq(ID_100), eq(1L), any()))
                    .thenReturn(bookingResponseDto);

            mockMvc.perform(post("/rides/" + ID_100 + "/bookings/1/confirm")
                            .with(csrf())
                            .with(authentication(userAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("403 when not driver")
        void confirmBooking_NotDriver() throws Exception {
            when(bookingService.confirmBooking(eq(ID_100), eq(1L), any()))
                    .thenThrow(new NotRideDriverException(ID_100, 2L));

            mockMvc.perform(post("/rides/" + ID_100 + "/bookings/1/confirm")
                            .with(csrf())
                            .with(authentication(userAuth())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("409 on invalid transition")
        void confirmBooking_InvalidTransition() throws Exception {
            when(bookingService.confirmBooking(eq(ID_100), eq(1L), any()))
                    .thenThrow(new InvalidBookingTransitionException(1L, BookingStatus.CONFIRMED, BookingStatus.CONFIRMED));

            mockMvc.perform(post("/rides/" + ID_100 + "/bookings/1/confirm")
                            .with(csrf())
                            .with(authentication(userAuth())))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /rides/{rideId}/bookings/{bookingId}/cancel")
    class CancelBookingTests {

        @Test
        @DisplayName("200 OK on cancel")
        void cancelBooking_Success() throws Exception {
            when(bookingService.cancelBooking(eq(ID_100), eq(1L), any(), any()))
                    .thenReturn(bookingResponseDto);

            mockMvc.perform(post("/rides/" + ID_100 + "/bookings/1/cancel")
                            .with(csrf())
                            .with(authentication(userAuth()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\": \"No longer need this ride\"}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /me/bookings")
    class GetMyBookingsTests {

        @Test
        @DisplayName("200 OK returns list")
        void getMyBookings_Success() throws Exception {
            when(bookingService.getMyBookings(any()))
                    .thenReturn(List.of(bookingResponseDto));

            mockMvc.perform(get("/me/bookings")
                            .with(authentication(userAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
        }
    }

    @Nested
    @DisplayName("GET /rides/{rideId}/bookings")
    class GetBookingsForRideTests {

        @Test
        @DisplayName("200 OK for driver")
        void getBookings_Success() throws Exception {
            when(bookingService.getBookingsForRide(eq(ID_100), any()))
                    .thenReturn(List.of(bookingResponseDto));

            mockMvc.perform(get("/rides/" + ID_100 + "/bookings")
                            .with(authentication(userAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("403 for non-driver")
        void getBookings_NotDriver() throws Exception {
            when(bookingService.getBookingsForRide(eq(ID_100), any()))
                    .thenThrow(new NotRideDriverException(ID_100, 2L));

            mockMvc.perform(get("/rides/" + ID_100 + "/bookings")
                            .with(authentication(userAuth())))
                    .andExpect(status().isForbidden());
        }
    }
}

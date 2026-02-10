package com.blablatwo.ride;

import com.blablatwo.auth.AppPrincipal;
import com.blablatwo.auth.service.JwtTokenProvider;
import com.blablatwo.exceptions.AlreadyBookedException;
import com.blablatwo.exceptions.BookingNotFoundException;
import com.blablatwo.exceptions.NoSuchRideException;
import com.blablatwo.exceptions.RideFullException;
import com.blablatwo.exceptions.RideNotBookableException;
import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.ride.dto.RideSearchCriteriaDto;
import com.blablatwo.user.Role;
import com.blablatwo.user.UserAccountRepository;
import com.blablatwo.user.exception.NoSuchUserException;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.blablatwo.util.Constants.*;
import static com.blablatwo.util.TestFixtures.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(controllers = RidesController.class)
class RidesControllerTest {

    private static final String BASE_URL = "/rides";

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RideService rideService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserAccountRepository userAccountRepository;

    private Ride ride;
    private RideCreationDto rideCreationDTO;
    private RideResponseDto rideResponseDto;

    @BeforeEach
    void setUp() {
        ride = aRide().build();
        rideCreationDTO = aRideCreationDto().build();
        rideResponseDto = aRideResponseDto().build();
    }

    @Test
    @DisplayName("GET /rides/{id} - Found")
    @WithMockUser
    void getRideById() throws Exception {
        // Arrange
        when(rideService.getById(ID_100)).thenReturn(Optional.of(rideResponseDto));

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/" + ID_100)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID_100))
                .andExpect(jsonPath("$.origin.name").value(CITY_NAME_ORIGIN))
                .andExpect(jsonPath("$.driver.name").value(CRISTIANO))
                .andExpect(jsonPath("$.seatsTaken").value(0))
                .andExpect(jsonPath("$.contactMethods[0].type").value("PHONE"));
    }

    @Test
    @DisplayName("GET /rides/{id} - Not Found")
    @WithMockUser
    void getRideById_NotFound() throws Exception {
        // Arrange
        when(rideService.getById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/" + NON_EXISTENT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /rides - Create ride - Success")
    @WithMockUser
    void testCreateRide_Success() throws Exception {
        // Arrange
        when(rideService.createForCurrentUser(any(), any())).thenReturn(rideResponseDto);

        // Act & Assert
        AppPrincipal principal = new AppPrincipal(ID_ONE, TRAVELER_EMAIL_USER1, Set.of(Role.USER));
        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.roles())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(rideCreationDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(ID_100));
    }

    @Test
    @DisplayName("POST /rides - Create ride - Validation Error")
    @WithMockUser
    void testCreateRide_ValidationError() throws Exception {
        // Arrange - invalid DTO
        RideCreationDto invalidRide = new RideCreationDto(
                null,    // originPlaceId - null (invalid)
                null,    // destinationPlaceId - null (invalid)
                null,    // departureTime - null (invalid)
                false, // isApproximate
                0,
                BigDecimal.valueOf(-1),
                null,
                ""
        );

        // Act & Assert
        AppPrincipal principal = new AppPrincipal(ID_ONE, TRAVELER_EMAIL_USER1, Set.of(Role.USER));
        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.roles())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(invalidRide)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /rides/{id} - Delete ride - Success")
    @WithMockUser
    void testDeleteRide_Success() throws Exception {
        // Arrange
        doNothing().when(rideService).delete(ID_100);

        // Act & Assert
        mockMvc.perform(delete(BASE_URL + "/" + ID_100)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /rides/{id} - Delete ride - Not Found")
    @WithMockUser
    void testDeleteRide_NotFound() throws Exception {
        // Arrange
        doThrow(new NoSuchRideException(NON_EXISTENT_ID)).when(rideService).delete(NON_EXISTENT_ID);

        // Act & Assert
        mockMvc.perform(delete(BASE_URL + "/" + NON_EXISTENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /rides/{id} - Update ride - Success")
    @WithMockUser
    void updateRide_Success() throws Exception {
        // Arrange
        when(rideService.update(any(RideCreationDto.class), eq(ID_100)))
                .thenReturn(rideResponseDto);

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/" + ID_100)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(rideCreationDTO)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(ID_100));
    }

    @Test
    @DisplayName("PUT /rides/{id} - Update ride - Not Found (ETag removed)")
    @WithMockUser
    void updateRide_NotFound() throws Exception {
        // Arrange
        when(rideService.update(any(RideCreationDto.class), eq(NON_EXISTENT_ID)))
                .thenThrow(new NoSuchRideException(NON_EXISTENT_ID));

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/" + NON_EXISTENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(rideCreationDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /rides/{id} - Update ride - Validation Error (ETag removed)")
    @WithMockUser
    void updateRide_ValidationError() throws Exception {
        // Arrange - invalid DTO
        RideCreationDto invalidRide = new RideCreationDto(
                null,    // originPlaceId - null (invalid)
                null,    // destinationPlaceId - null (invalid)
                null,    // departureTime - null (invalid)
                false, // isApproximate
                0,
                BigDecimal.valueOf(-1),
                null,
                ""
        );

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/" + ID_100)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(invalidRide)))
                .andExpect(status().isBadRequest());
    }

    @Nested
    @DisplayName("GET /rides - List All Rides")
    class GetAllRidesTests {

        @Test
        @DisplayName("GET /rides - Returns paginated rides")
        @WithMockUser
        void getAllRides_Success() throws Exception {
            // Arrange
            Page<RideResponseDto> ridePage = new PageImpl<>(List.of(rideResponseDto));
            when(rideService.getAllRides(any(Pageable.class))).thenReturn(ridePage);

            // Act & Assert
            mockMvc.perform(get(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].id").value(ID_100))
                    .andExpect(jsonPath("$.content[0].driver.name").value(CRISTIANO))
                    .andExpect(jsonPath("$.content[0].seatsTaken").value(0));
        }

        @Test
        @DisplayName("GET /rides - Returns empty page when no rides")
        @WithMockUser
        void getAllRides_EmptyPage() throws Exception {
            // Arrange
            Page<RideResponseDto> emptyPage = new PageImpl<>(Collections.emptyList());
            when(rideService.getAllRides(any(Pageable.class))).thenReturn(emptyPage);

            // Act & Assert
            mockMvc.perform(get(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /rides/search - Search Rides")
    class SearchRidesTests {

        @Test
        @DisplayName("GET /rides/search - Returns matching rides")
        @WithMockUser
        void searchRides_Success() throws Exception {
            // Arrange
            Page<RideResponseDto> ridePage = new PageImpl<>(List.of(rideResponseDto));
            when(rideService.searchRides(any(RideSearchCriteriaDto.class), any(Pageable.class)))
                    .thenReturn(ridePage);

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/search")
                            .param("originPlaceId", ID_ONE.toString())
                            .param("destinationPlaceId", "2")
                            .param("lang", "pl")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].id").value(ID_100))
                    .andExpect(jsonPath("$.content[0].driver.name").value(CRISTIANO));
        }

        @Test
        @DisplayName("GET /rides/search - Returns empty results when no matches")
        @WithMockUser
        void searchRides_NoMatches() throws Exception {
            // Arrange
            Page<RideResponseDto> emptyPage = new PageImpl<>(Collections.emptyList());
            when(rideService.searchRides(any(RideSearchCriteriaDto.class), any(Pageable.class)))
                    .thenReturn(emptyPage);

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/search")
                            .param("originPlaceId", "999999")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("GET /rides/search - Works with date parameter")
        @WithMockUser
        void searchRides_WithDate() throws Exception {
            // Arrange
            Page<RideResponseDto> ridePage = new PageImpl<>(List.of(rideResponseDto));
            when(rideService.searchRides(any(RideSearchCriteriaDto.class), any(Pageable.class)))
                    .thenReturn(ridePage);

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/search")
                            .param("departureDate", "2025-09-12")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Nested
    @DisplayName("POST /rides/{rideId}/book - Book Ride")
    class BookRideTests {

        private static final Long PASSENGER_ID = 2L;

        @Test
        @DisplayName("POST /rides/{id}/book - Book ride successfully")
        void bookRide_Success() throws Exception {
            // Arrange
            when(rideService.bookRide(any(), any())).thenReturn(rideResponseDto);
            AppPrincipal principal = new AppPrincipal(PASSENGER_ID, TRAVELER_EMAIL_USER2, Set.of(Role.USER));

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/" + ID_100 + "/book")
                            .with(csrf())
                            .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.roles())))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID_100));
        }

        @Test
        @DisplayName("POST /rides/{id}/book - Ride not found")
        void bookRide_RideNotFound() throws Exception {
            // Arrange
            when(rideService.bookRide(any(), any()))
                    .thenThrow(new NoSuchRideException(NON_EXISTENT_ID));
            AppPrincipal principal = new AppPrincipal(PASSENGER_ID, TRAVELER_EMAIL_USER2, Set.of(Role.USER));

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/" + NON_EXISTENT_ID + "/book")
                            .with(csrf())
                            .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.roles())))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("POST /rides/{id}/book - User not found")
        void bookRide_UserNotFound() throws Exception {
            // Arrange
            when(rideService.bookRide(any(), any()))
                    .thenThrow(new NoSuchUserException(NON_EXISTENT_ID));
            AppPrincipal principal = new AppPrincipal(NON_EXISTENT_ID, "unknown@test.com", Set.of(Role.USER));

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/" + ID_100 + "/book")
                            .with(csrf())
                            .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.roles())))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("POST /rides/{id}/book - Ride is full")
        void bookRide_RideFull() throws Exception {
            // Arrange
            when(rideService.bookRide(any(), any()))
                    .thenThrow(new RideFullException(ID_100));
            AppPrincipal principal = new AppPrincipal(PASSENGER_ID, TRAVELER_EMAIL_USER2, Set.of(Role.USER));

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/" + ID_100 + "/book")
                            .with(csrf())
                            .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.roles())))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("POST /rides/{id}/book - Already booked")
        void bookRide_AlreadyBooked() throws Exception {
            // Arrange
            when(rideService.bookRide(any(), any()))
                    .thenThrow(new AlreadyBookedException(ID_100, PASSENGER_ID));
            AppPrincipal principal = new AppPrincipal(PASSENGER_ID, TRAVELER_EMAIL_USER2, Set.of(Role.USER));

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/" + ID_100 + "/book")
                            .with(csrf())
                            .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.roles())))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("POST /rides/{id}/book - Ride not bookable")
        void bookRide_NotBookable() throws Exception {
            // Arrange
            when(rideService.bookRide(any(), any()))
                    .thenThrow(new RideNotBookableException(ID_100, "CANCELLED"));
            AppPrincipal principal = new AppPrincipal(PASSENGER_ID, TRAVELER_EMAIL_USER2, Set.of(Role.USER));

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/" + ID_100 + "/book")
                            .with(csrf())
                            .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.roles())))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /rides/{rideId}/book - Cancel Booking")
    class CancelBookingTests {

        private static final Long PASSENGER_ID = 2L;

        @Test
        @DisplayName("DELETE /rides/{id}/book - Cancel booking successfully")
        void cancelBooking_Success() throws Exception {
            // Arrange
            when(rideService.cancelBooking(any(), any())).thenReturn(rideResponseDto);
            AppPrincipal principal = new AppPrincipal(PASSENGER_ID, TRAVELER_EMAIL_USER2, Set.of(Role.USER));

            // Act & Assert
            mockMvc.perform(delete(BASE_URL + "/" + ID_100 + "/book")
                            .with(csrf())
                            .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.roles())))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID_100));
        }

        @Test
        @DisplayName("DELETE /rides/{id}/book - Booking not found")
        void cancelBooking_NotFound() throws Exception {
            // Arrange
            when(rideService.cancelBooking(any(), any()))
                    .thenThrow(new BookingNotFoundException(ID_100, PASSENGER_ID));
            AppPrincipal principal = new AppPrincipal(PASSENGER_ID, TRAVELER_EMAIL_USER2, Set.of(Role.USER));

            // Act & Assert
            mockMvc.perform(delete(BASE_URL + "/" + ID_100 + "/book")
                            .with(csrf())
                            .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.roles())))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE /rides/{id}/book - Ride not found")
        void cancelBooking_RideNotFound() throws Exception {
            // Arrange
            when(rideService.cancelBooking(any(), any()))
                    .thenThrow(new NoSuchRideException(NON_EXISTENT_ID));
            AppPrincipal principal = new AppPrincipal(PASSENGER_ID, TRAVELER_EMAIL_USER2, Set.of(Role.USER));

            // Act & Assert
            mockMvc.perform(delete(BASE_URL + "/" + NON_EXISTENT_ID + "/book")
                            .with(csrf())
                            .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.roles())))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /me/rides - My Rides")
    class GetMyRidesTests {

        private static final Long USER_ID = 2L;

        @Test
        @DisplayName("GET /me/rides - Returns user's rides")
        void getMyRides_Success() throws Exception {
            // Arrange
            when(rideService.getRidesForPassenger(any())).thenReturn(List.of(rideResponseDto));
            AppPrincipal principal = new AppPrincipal(USER_ID, TRAVELER_EMAIL_USER2, Set.of(Role.USER));

            // Act & Assert
            mockMvc.perform(get("/me/rides")
                            .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.roles())))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(ID_100))
                    .andExpect(jsonPath("$[0].driver.name").value(CRISTIANO));
        }

        @Test
        @DisplayName("GET /me/rides - Returns empty list when no bookings")
        void getMyRides_Empty() throws Exception {
            // Arrange
            when(rideService.getRidesForPassenger(any())).thenReturn(Collections.emptyList());
            AppPrincipal principal = new AppPrincipal(USER_ID, TRAVELER_EMAIL_USER2, Set.of(Role.USER));

            // Act & Assert
            mockMvc.perform(get("/me/rides")
                            .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.roles())))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("GET /me/rides - User not found")
        void getMyRides_UserNotFound() throws Exception {
            // Arrange
            when(rideService.getRidesForPassenger(any()))
                    .thenThrow(new NoSuchUserException(NON_EXISTENT_ID));
            AppPrincipal principal = new AppPrincipal(NON_EXISTENT_ID, "unknown@test.com", Set.of(Role.USER));

            // Act & Assert
            mockMvc.perform(get("/me/rides")
                            .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.roles())))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }
}

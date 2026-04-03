package com.vamigo.ride;

import com.vamigo.auth.AppPrincipal;
import com.vamigo.domain.TimePrecision;
import com.vamigo.auth.service.JwtTokenProvider;
import com.vamigo.exceptions.NoSuchRideException;
import com.vamigo.exceptions.NotRideDriverException;
import com.vamigo.ride.dto.RideCreationDto;
import com.vamigo.ride.dto.RideResponseDto;
import com.vamigo.ride.dto.RideSearchCriteriaDto;
import com.vamigo.user.Role;
import com.vamigo.user.UserAccountRepository;
import com.vamigo.user.exception.NoSuchUserException;
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

import static com.vamigo.util.Constants.*;
import static com.vamigo.util.TestFixtures.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

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
                .andExpect(jsonPath("$.origin.name").value(LOCATION_NAME_ORIGIN))
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
                null,    // origin - null (invalid)
                null,    // destination - null (invalid)
                null,    // intermediateStops
                null,    // departureTime - null (invalid)
                TimePrecision.EXACT,   // timePrecision
                0,       // availableSeats
                BigDecimal.valueOf(-1),
                null,    // vehicleId
                "",      // description
                true,    // autoApprove
                false,   // doorToDoor
                null,    // contactPhone
                null,    // currency
                null     // originLegPrice
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
    void testDeleteRide_Success() throws Exception {
        // Arrange
        AppPrincipal principal = new AppPrincipal(ID_ONE, TRAVELER_EMAIL_USER1, Set.of(Role.USER));
        doNothing().when(rideService).delete(eq(ID_100), any());

        // Act & Assert
        mockMvc.perform(delete(BASE_URL + "/" + ID_100)
                        .with(csrf())
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.roles())))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /rides/{id} - Delete ride - Not Found")
    void testDeleteRide_NotFound() throws Exception {
        // Arrange
        AppPrincipal principal = new AppPrincipal(ID_ONE, TRAVELER_EMAIL_USER1, Set.of(Role.USER));
        doThrow(new NoSuchRideException(NON_EXISTENT_ID)).when(rideService).delete(eq(NON_EXISTENT_ID), any());

        // Act & Assert
        mockMvc.perform(delete(BASE_URL + "/" + NON_EXISTENT_ID)
                        .with(csrf())
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.roles())))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /rides/{id} - Not Owner - 403")
    void testDeleteRide_NotOwner() throws Exception {
        // Arrange - service throws NotRideDriverException
        AppPrincipal principal = new AppPrincipal(2L, TRAVELER_EMAIL_USER2, Set.of(Role.USER));
        doThrow(new NotRideDriverException(ID_100, 2L)).when(rideService).delete(eq(ID_100), any());

        // Act & Assert
        mockMvc.perform(delete(BASE_URL + "/" + ID_100)
                        .with(csrf())
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.roles())))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /rides/{id} - Not Owner - 403")
    void updateRide_NotOwner() throws Exception {
        // Arrange - service throws NotRideDriverException
        AppPrincipal principal = new AppPrincipal(2L, TRAVELER_EMAIL_USER2, Set.of(Role.USER));
        when(rideService.update(any(RideCreationDto.class), eq(ID_100), any()))
                .thenThrow(new NotRideDriverException(ID_100, 2L));

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/" + ID_100)
                        .with(csrf())
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.roles())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(rideCreationDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /rides/{id} - Update ride - Success")
    void updateRide_Success() throws Exception {
        // Arrange
        when(rideService.update(any(RideCreationDto.class), eq(ID_100), any()))
                .thenReturn(rideResponseDto);
        AppPrincipal principal = new AppPrincipal(ID_ONE, TRAVELER_EMAIL_USER1, Set.of(Role.USER));

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/" + ID_100)
                        .with(csrf())
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.roles())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(rideCreationDTO)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(ID_100));
    }

    @Test
    @DisplayName("PUT /rides/{id} - Update ride - Not Found (ETag removed)")
    void updateRide_NotFound() throws Exception {
        // Arrange
        when(rideService.update(any(RideCreationDto.class), eq(NON_EXISTENT_ID), any()))
                .thenThrow(new NoSuchRideException(NON_EXISTENT_ID));
        AppPrincipal principal = new AppPrincipal(ID_ONE, TRAVELER_EMAIL_USER1, Set.of(Role.USER));

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/" + NON_EXISTENT_ID)
                        .with(csrf())
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.roles())))
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
                null,    // origin - null (invalid)
                null,    // destination - null (invalid)
                null,    // intermediateStops
                null,    // departureTime - null (invalid)
                TimePrecision.EXACT,   // timePrecision
                0,       // availableSeats
                BigDecimal.valueOf(-1),
                null,    // vehicleId
                "",      // description
                true,    // autoApprove
                false,   // doorToDoor
                null,    // contactPhone
                null,    // currency
                null     // originLegPrice
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
                            .param("originOsmId", OSM_ID_ORIGIN.toString())
                            .param("destinationOsmId", OSM_ID_DESTINATION.toString())
                            .param("earliestDeparture", FUTURE_DEPARTURE.toString())
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
                            .param("originOsmId", "999999")
                            .param("earliestDeparture", FUTURE_DEPARTURE.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("GET /rides/search - Works with time window parameters")
        @WithMockUser
        void searchRides_WithTimeWindow() throws Exception {
            // Arrange
            Page<RideResponseDto> ridePage = new PageImpl<>(List.of(rideResponseDto));
            when(rideService.searchRides(any(RideSearchCriteriaDto.class), any(Pageable.class)))
                    .thenReturn(ridePage);

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/search")
                            .param("earliestDeparture", "2025-09-12T08:00:00Z")
                            .param("latestDeparture", "2025-09-13T00:00:00Z")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
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
            when(rideService.getRidesForDriver(any())).thenReturn(List.of(rideResponseDto));
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
            when(rideService.getRidesForDriver(any())).thenReturn(Collections.emptyList());
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
            when(rideService.getRidesForDriver(any()))
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

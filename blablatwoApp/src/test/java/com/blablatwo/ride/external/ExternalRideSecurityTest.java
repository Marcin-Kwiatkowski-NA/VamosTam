package com.blablatwo.ride.external;

import com.blablatwo.AbstractIntegrationTest;
import com.blablatwo.ride.dto.ExternalRideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

import static com.blablatwo.util.TestFixtures.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExternalRideSecurityTest extends AbstractIntegrationTest {

    private static final String BASE_URL = "/rides/external";
    private static final String VALID_API_KEY = "test-api-key-12345";
    private static final String INVALID_API_KEY = "wrong-api-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private ExternalRideService externalRideService;

    private ExternalRideCreationDto validDto;
    private RideResponseDto responseDto;

    @BeforeEach
    void setUp() {
        validDto = aFrenchRideCreation().build();
        responseDto = aFrenchRideResponse().id(1L).build();
    }

    @Test
    @DisplayName("POST /rides/external returns 401 without API key")
    void createExternalRide_withoutApiKey_returns401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(validDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /rides/external returns 401 with wrong API key")
    void createExternalRide_withWrongApiKey_returns401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .header("X-API-Key", INVALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(validDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /rides/external succeeds with valid API key")
    void createExternalRide_withValidApiKey_succeeds() throws Exception {
        when(externalRideService.createExternalRide(any(ExternalRideCreationDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post(BASE_URL)
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(validDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("GET /rides/external/{externalId} returns 401 without API key")
    void getExternalRide_withoutApiKey_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/fb-12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /rides/external/{externalId} works with valid API key")
    void getExternalRide_withValidApiKey_succeeds() throws Exception {
        when(externalRideService.getByExternalId("fb-12345"))
                .thenReturn(Optional.of(responseDto));

        mockMvc.perform(get(BASE_URL + "/fb-12345")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("GET /rides/external/check/{externalId} returns 401 without API key")
    void checkExists_withoutApiKey_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/check/fb-12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /rides/external/check/{externalId} works with valid API key")
    void checkExists_withValidApiKey_succeeds() throws Exception {
        when(externalRideService.existsByExternalId("fb-12345"))
                .thenReturn(true);

        mockMvc.perform(get(BASE_URL + "/check/fb-12345")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    @DisplayName("JWT token does NOT work for /rides/external - returns 401")
    void createExternalRide_withJwt_returns401() throws Exception {
        // JWT should not work for external rides endpoint - only API key is accepted
        // Using a fake JWT token (the real validation will fail anyway since we're testing
        // that the external endpoint filter chain doesn't accept JWT at all)
        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer fake.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(validDto)))
                .andExpect(status().isUnauthorized());
    }
}

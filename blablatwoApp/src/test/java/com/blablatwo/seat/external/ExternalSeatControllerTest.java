package com.blablatwo.seat.external;

import com.blablatwo.auth.service.JwtTokenProvider;
import com.blablatwo.city.CityDto;
import com.blablatwo.city.Lang;
import com.blablatwo.dto.ContactMethodDto;
import com.blablatwo.dto.ContactType;
import com.blablatwo.dto.UserCardDto;
import com.blablatwo.ride.RideSource;
import com.blablatwo.seat.SeatStatus;
import com.blablatwo.seat.dto.ExternalSeatCreationDto;
import com.blablatwo.seat.dto.SeatResponseDto;
import com.blablatwo.user.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.blablatwo.util.Constants.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExternalSeatController.class)
class ExternalSeatControllerTest {

    private static final String BASE_URL = "/seats/external";
    private static final String EXTERNAL_ID = "fb-seat-12345";

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExternalSeatService externalSeatService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserAccountRepository userAccountRepository;

    private ExternalSeatCreationDto creationDto;
    private SeatResponseDto seatResponseDto;

    @BeforeEach
    void setUp() {
        creationDto = new ExternalSeatCreationDto(
                CITY_NAME_ORIGIN,
                CITY_NAME_DESTINATION,
                Lang.PL,
                LOCAL_DATE,
                LOCAL_TIME,
                false,
                ONE,
                BIG_DECIMAL,
                "Looking for a ride",
                EXTERNAL_ID,
                "raw content",
                TELEPHONE,
                CRISTIANO,
                "https://facebook.com/post/12345"
        );

        CityDto originCityDto = new CityDto(ID_ONE, CITY_NAME_ORIGIN, "PL", 100000L);
        CityDto destinationCityDto = new CityDto(2L, CITY_NAME_DESTINATION, "PL", 200000L);

        seatResponseDto = SeatResponseDto.builder()
                .id(ID_100)
                .source(RideSource.FACEBOOK)
                .origin(originCityDto)
                .destination(destinationCityDto)
                .departureTime(LOCAL_DATE.atTime(LOCAL_TIME))
                .isApproximate(false)
                .count(ONE)
                .priceWillingToPay(BIG_DECIMAL)
                .description("Looking for a ride")
                .passenger(new UserCardDto(ID_ONE, CRISTIANO, null, null))
                .contactMethods(List.of(new ContactMethodDto(ContactType.PHONE, TELEPHONE)))
                .seatStatus(SeatStatus.SEARCHING)
                .build();
    }

    @Test
    @DisplayName("POST /seats/external - Create external seat - Success")
    @WithMockUser
    void createExternalSeat_Success() throws Exception {
        when(externalSeatService.createExternalSeat(any())).thenReturn(seatResponseDto);

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(creationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID_100))
                .andExpect(jsonPath("$.source").value("FACEBOOK"))
                .andExpect(jsonPath("$.origin.name").value(CITY_NAME_ORIGIN))
                .andExpect(jsonPath("$.passenger.name").value(CRISTIANO))
                .andExpect(jsonPath("$.seatStatus").value("SEARCHING"));
    }

    @Test
    @DisplayName("POST /seats/external - Validation error")
    @WithMockUser
    void createExternalSeat_ValidationError() throws Exception {
        ExternalSeatCreationDto invalidDto = new ExternalSeatCreationDto(
                null, null, null, null, null,
                false, 0, null, null,
                null, null, null, null, null
        );

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /seats/external/check/{externalId} - Exists")
    @WithMockUser
    void checkExists_True() throws Exception {
        when(externalSeatService.existsByExternalId(EXTERNAL_ID)).thenReturn(true);

        mockMvc.perform(get(BASE_URL + "/check/" + EXTERNAL_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    @DisplayName("GET /seats/external/check/{externalId} - Does not exist")
    @WithMockUser
    void checkExists_False() throws Exception {
        when(externalSeatService.existsByExternalId(EXTERNAL_ID)).thenReturn(false);

        mockMvc.perform(get(BASE_URL + "/check/" + EXTERNAL_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }

    @Test
    @DisplayName("GET /seats/external/{externalId} - Found")
    @WithMockUser
    void getByExternalId_Found() throws Exception {
        when(externalSeatService.getByExternalId(EXTERNAL_ID)).thenReturn(Optional.of(seatResponseDto));

        mockMvc.perform(get(BASE_URL + "/" + EXTERNAL_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID_100))
                .andExpect(jsonPath("$.origin.name").value(CITY_NAME_ORIGIN));
    }

    @Test
    @DisplayName("GET /seats/external/{externalId} - Not found")
    @WithMockUser
    void getByExternalId_NotFound() throws Exception {
        when(externalSeatService.getByExternalId("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_URL + "/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}

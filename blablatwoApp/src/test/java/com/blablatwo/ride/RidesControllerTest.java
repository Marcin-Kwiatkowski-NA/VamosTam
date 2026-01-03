package com.blablatwo.ride;

import com.blablatwo.city.City;
import com.blablatwo.city.CityDto;
import com.blablatwo.exceptions.NoSuchRideException;
import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.traveler.DriverProfileDto;
import com.blablatwo.traveler.Traveler;
import com.blablatwo.vehicle.Vehicle;
import com.blablatwo.vehicle.VehicleResponseDto;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static com.blablatwo.util.Constants.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(RidesController.class)
class RidesControllerTest {

    private static final String BASE_URL = "/rides";

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RideService rideService;

    private Ride ride;
    private RideCreationDto rideCreationDTO;
    private RideResponseDto rideResponseDto;

    @BeforeEach
    void setUp() {
        ride = Ride.builder()
                .id(ID_100)
                .driver(Traveler.builder().id(ID_ONE).username(TRAVELER_USERNAME_USER1).build())
                .origin(City.builder().id(ID_ONE).osmId(ID_ONE).name(CITY_NAME_ORIGIN).build())
                .destination(City.builder().id(2L).osmId(2L).name(CITY_NAME_DESTINATION).build())
                .departureTime(LOCAL_DATE_TIME)
                .availableSeats(ONE)
                .pricePerSeat(BIG_DECIMAL)
                .vehicle(Vehicle.builder().id(ID_ONE).licensePlate(VEHICLE_LICENSE_PLATE_1).build())
                .rideStatus(RideStatus.OPEN)
                .lastModified(INSTANT)
                .passengers(Collections.emptyList())
                .description(RIDE_DESCRIPTION)
                .build();

        rideCreationDTO = new RideCreationDto(
                ID_ONE,
                new CityDto(ID_ONE, CITY_NAME_ORIGIN),
                new CityDto(2L, CITY_NAME_DESTINATION),
                LOCAL_DATE_TIME,
                ONE,
                BIG_DECIMAL,
                ID_ONE,
                RIDE_DESCRIPTION
        );

        rideResponseDto = new RideResponseDto(
                ID_100,
                new DriverProfileDto(ID_ONE, TRAVELER_USERNAME_USER1, EMAIL, TELEPHONE, CRISTIANO),
                new CityDto(ID_ONE, CITY_NAME_ORIGIN),
                new CityDto(2L, CITY_NAME_DESTINATION),
                LOCAL_DATE_TIME,
                ONE,
                BIG_DECIMAL,
                new VehicleResponseDto(ID_ONE, VEHICLE_MAKE_TESLA, VEHICLE_MODEL_MODEL_S, VEHICLE_PRODUCTION_YEAR_2021, VEHICLE_COLOR_RED, VEHICLE_LICENSE_PLATE_1),
                RideStatus.OPEN,
                INSTANT,
                Collections.emptyList()
        );
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
                .andExpect(jsonPath("$.origin.name").value(CITY_NAME_ORIGIN));
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
        when(rideService.create(any(RideCreationDto.class))).thenReturn(rideResponseDto);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
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
        // Arrange
        RideCreationDto invalidRide = new RideCreationDto(
                null,
                new CityDto(null, ""),
                new CityDto(null, ""),
                LocalDateTime.now().minusDays(1),
                0,
                BigDecimal.valueOf(-1),
                null,
                ""
        );

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
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
        // Arrange
        RideCreationDto invalidRide = new RideCreationDto(
                null,
                new CityDto(null, ""),
                new CityDto(null, ""),
                LocalDateTime.now().minusDays(1),
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
}
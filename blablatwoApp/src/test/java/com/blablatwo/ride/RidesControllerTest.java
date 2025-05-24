package com.blablatwo.ride;

import com.blablatwo.exceptions.ETagMismatchException;
import com.blablatwo.exceptions.NoSuchRideException;
import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.blablatwo.util.Constants.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(RidesController.class)
class RidesControllerTest {

    private static final String BASE_URL = "/rides";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RideService rideService;

    private Ride ride;
    private RideCreationDto rideCreationDTO;
    RideResponseDto rideResponseDto;

    @BeforeEach
    void setUp() {
        ride = Ride.builder()
                .id(ID_100)
                .lastModified(INSTANT)
                .build();

        rideCreationDTO = new RideCreationDto(
                ID_ONE, ID_100, LOCAL_DATE_TIME, ONE, BIG_DECIMAL, ID_100, List.of(ID_100)
        );

        rideResponseDto = new RideResponseDto(
                ID_100, null, null, null, null, null, LOCAL_DATE_TIME,
                ONE, BIG_DECIMAL, null, INSTANT, null);
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
                .andExpect(jsonPath("$.id").value(ID_100));

    }

    @Test
    @DisplayName("GET /rides/{id} - Not Found")
    @WithMockUser
    void GetRideById_NotFound() throws Exception {
        // Arrange
        when(rideService.getById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/" + NON_EXISTENT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Create ride - Success")
    @WithMockUser
    void testCreateRide_Success() throws Exception {
        // Arrange
        when(rideService.create(rideCreationDTO)).thenReturn(rideResponseDto);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rideCreationDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
//                .andExpect(header().string("ETag", String.valueOf(ETAG)))
                .andExpect(jsonPath("$.id").value(ID_100));
    }

    @Test
    @DisplayName("Create ride - Validation Error")
    @WithMockUser
    void testCreateRide_ValidationError() throws Exception {
        // Arrange
        RideCreationDto invalidRide = new RideCreationDto(
                0L,0L, LocalDateTime.MIN, 0, BigDecimal.ZERO, 0L,null
        );

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRide)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Delete ride - Success")
    @WithMockUser
    public void testDeleteRide_Success() throws Exception {
        // Arrange
        doNothing().when(rideService).delete(ID_100);

        // Act & Assert
        mockMvc.perform(delete(BASE_URL + "/" + ID_100)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Delete ride - Not Found")
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
        String ifMatch = ETAG;
        when(rideService.update(any(RideCreationDto.class), eq(ID_100), eq(ifMatch)))
                .thenReturn(rideResponseDto);

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/" + ID_100)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", ifMatch)
                        .content(objectMapper.writeValueAsString(rideCreationDTO)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Location"))
//                .andExpect(header().string("ETag", ETAG))
                .andExpect(jsonPath("$.id").value(ID_100));
    }

    @Test
    @DisplayName("PUT /rides/{id} - Update ride - Not Found")
    @WithMockUser
    void updateRide_NotFound() throws Exception {
        // Arrange
        String ifMatch = ETAG;
        when(rideService.update(any(RideCreationDto.class), eq(NON_EXISTENT_ID), eq(ifMatch)))
                .thenThrow(new NoSuchRideException(NON_EXISTENT_ID));

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/" + NON_EXISTENT_ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", ifMatch)
                        .content(objectMapper.writeValueAsString(rideCreationDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /rides/{id} - Update ride - ETag Mismatch")
    @WithMockUser
    void updateRide_ETagMismatch() throws Exception {
        // Arrange
        String ifMatch = ETAG;
        when(rideService.update(any(RideCreationDto.class), eq(ID_100), eq(ifMatch)))
                .thenThrow(new ETagMismatchException());

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/" + ID_100)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", ifMatch)
                        .content(objectMapper.writeValueAsString(rideCreationDTO)))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    @DisplayName("PUT /rides/{id} - Update ride - Validation Error")
    @WithMockUser
    void updateRide_ValidationError() throws Exception {
        // Arrange
        String ifMatch = ETAG;
        RideCreationDto invalidRide = new RideCreationDto(
                0L,0L, LocalDateTime.MIN, 0, BigDecimal.ZERO, 0L,null
        );

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/" + ID_100)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", ifMatch)
                        .content(objectMapper.writeValueAsString(invalidRide)))
                .andExpect(status().isBadRequest());
    }
}

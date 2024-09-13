package mkpw.blablatwo.controllers;

import static mkpw.blablatwo.utils.ConstantsTest.RIDE_ID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


import mkpw.blablatwo.hateoas.RideRepresentationModelAssembler;
import mkpw.blablatwo.services.ride.RideService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RidesController.class)
class RidesControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RideRepresentationModelAssembler rideAssembler;

    @MockBean
    private RideService rideService;

    @Test
    @DisplayName("Happy path: Returns OK Status for /rides/{rideId}")
    void getRideByIdTest() throws Exception {
        // Arrange
        when(rideService.getRideById(any())).thenReturn(null);
        when(rideAssembler.toModel(any())).thenReturn(null);

        // Act and Assert
        mockMvc.perform(get("/rides/" + RIDE_ID)
                        .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk());
    }
}
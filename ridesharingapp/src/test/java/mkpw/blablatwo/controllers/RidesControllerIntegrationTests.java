package mkpw.blablatwo.controllers;

import mkpw.blablatwo.entity.RideEntity;
import mkpw.blablatwo.model.Ride;
import mkpw.blablatwo.repository.RideRepository;
import mkpw.blablatwo.utils.TestBeans;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static mkpw.blablatwo.utils.ConstantsTest.DATE_TIME;
import static mkpw.blablatwo.utils.ConstantsTest.RIDE_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestBeans.class)
public class RidesControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RideEntity rideEntity;

    @Mock
    private RideRepository repository;

//    @Test
//    @DisplayName("Happy path: Returns  /rides/{rideId}")
//    void getRideByIdTest() throws Exception {
//        // Arrange
//
//        Ride ride = new Ride();
//
//        // Act and Assert
//        mockMvc.perform(get("/rides/" + RIDE_ID)
//                        .accept(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk());
//    }
//
//
//    @Test
//    @DisplayName("Returns 4xx status code if no such ride Id found")
//    void getRideByUnExistedIdTest() throws Exception {
//        // Arrange
//        when(repository.findById(any())).thenReturn(Optional.empty());
//        Ride ride = new Ride();
//
//        // Act and Assert
//        mockMvc.perform(get("/rides/" + RIDE_ID))
//                .andExpect(status().is4xxClientError());
//    }
}

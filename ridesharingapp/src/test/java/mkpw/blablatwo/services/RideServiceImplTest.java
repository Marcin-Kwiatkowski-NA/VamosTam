package mkpw.blablatwo.services;

import mkpw.blablatwo.entity.RideEntity;
import mkpw.blablatwo.exeptions.runtime.NoSuchRideException;
import mkpw.blablatwo.repository.RideRepository;
import mkpw.blablatwo.services.ride.RideServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static mkpw.blablatwo.utils.ConstantsTest.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideServiceImplTest {

    @Mock
    private RideRepository repository;

    @InjectMocks
    private RideServiceImpl underTest;


    @Test
    @DisplayName("Throws an exception when no such ride id found in repository")
    void NoSuchRideTest() {
        // GIVEN
        // WHEN
        when(repository.findById(any())).thenReturn(Optional.empty());
        // THEN
        assertThrows(NoSuchRideException.class,
                () -> underTest.getRideById(RIDE_ID));
    }

    @Test
    @DisplayName("Happy path: returns RideEntity by ride id")
    void getRideById() {
        // GIVEN
        RideEntity ride = new RideEntity();

        // WHEN
        when(repository.findById(any())).thenReturn(Optional.of(ride));
        var result = underTest.getRideById(RIDE_ID);

        // THEN
        assertEquals(ride, result);
    }

    @Test
    void getRideByIdd() {
        // GIVEN
        // WHEN
        // THEN
    }
}
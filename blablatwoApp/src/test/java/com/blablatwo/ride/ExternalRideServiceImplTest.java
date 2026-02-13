package com.blablatwo.ride;

import com.blablatwo.city.City;
import com.blablatwo.domain.ExternalImportSupport;
import com.blablatwo.exceptions.DuplicateExternalEntityException;
import com.blablatwo.ride.dto.ExternalRideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.user.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.blablatwo.util.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalRideServiceImplTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private RideExternalMetaRepository metaRepository;

    @Mock
    private ExternalImportSupport importSupport;

    @Mock
    private RideMapper rideMapper;

    @Mock
    private RideResponseEnricher rideResponseEnricher;

    @InjectMocks
    private ExternalRideServiceImpl externalRideService;

    private City originCity;
    private City destinationCity;
    private City intermediateCity;
    private UserAccount proxyUser;
    private RideResponseDto responseDto;

    @BeforeEach
    void setUp() {
        originCity = anOriginCity().build();
        destinationCity = aDestinationCity().build();
        intermediateCity = aKrakowCity().build();
        proxyUser = aDriverAccount().build();
        responseDto = aRideResponseDto().build();

        lenient().when(importSupport.resolveProxyUser()).thenReturn(proxyUser);
        lenient().when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(rideMapper.rideEntityToRideResponseDto(any())).thenReturn(responseDto);
        lenient().when(rideResponseEnricher.enrich(any(Ride.class), any(RideResponseDto.class)))
                .thenAnswer(inv -> inv.getArgument(1));
    }

    @Test
    @DisplayName("Create external ride without intermediate stops creates 2 stops")
    void createExternalRide_withNoIntermediateStops_creates2Stops() {
        // Arrange
        ExternalRideCreationDto dto = anExternalRideCreationDto().build();
        when(importSupport.resolveCities(dto.originCityName(), dto.destinationCityName(), "pl"))
                .thenReturn(new ExternalImportSupport.ResolvedCities(originCity, destinationCity));

        // Act
        externalRideService.createExternalRide(dto);

        // Assert
        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository, times(2)).save(rideCaptor.capture());

        Ride savedRide = rideCaptor.getAllValues().get(1);
        assertEquals(2, savedRide.getStops().size());
        assertEquals(0, savedRide.getStops().get(0).getStopOrder());
        assertEquals(originCity, savedRide.getStops().get(0).getCity());
        assertNotNull(savedRide.getStops().get(0).getDepartureTime());
        assertEquals(1, savedRide.getStops().get(1).getStopOrder());
        assertEquals(destinationCity, savedRide.getStops().get(1).getCity());
        assertNull(savedRide.getStops().get(1).getDepartureTime());
    }

    @Test
    @DisplayName("Create external ride with intermediate stops creates all stops")
    void createExternalRide_withIntermediateStops_createsAllStops() {
        // Arrange
        ExternalRideCreationDto dto = anExternalRideWithStops().build();
        when(importSupport.resolveCities(dto.originCityName(), dto.destinationCityName(), "pl"))
                .thenReturn(new ExternalImportSupport.ResolvedCities(originCity, destinationCity));
        when(importSupport.resolveCityByName("Kraków", "pl")).thenReturn(intermediateCity);

        // Act
        externalRideService.createExternalRide(dto);

        // Assert
        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository, times(2)).save(rideCaptor.capture());

        Ride savedRide = rideCaptor.getAllValues().get(1);
        assertEquals(3, savedRide.getStops().size());

        // Origin
        assertEquals(0, savedRide.getStops().get(0).getStopOrder());
        assertEquals(originCity, savedRide.getStops().get(0).getCity());
        assertNotNull(savedRide.getStops().get(0).getDepartureTime());

        // Intermediate — no departure time
        assertEquals(1, savedRide.getStops().get(1).getStopOrder());
        assertEquals(intermediateCity, savedRide.getStops().get(1).getCity());
        assertNull(savedRide.getStops().get(1).getDepartureTime());

        // Destination
        assertEquals(2, savedRide.getStops().get(2).getStopOrder());
        assertEquals(destinationCity, savedRide.getStops().get(2).getCity());
        assertNull(savedRide.getStops().get(2).getDepartureTime());
    }

    @Test
    @DisplayName("Create external ride with duplicate externalId throws exception")
    void createExternalRide_duplicateExternalId_throws() {
        // Arrange
        ExternalRideCreationDto dto = anExternalRideCreationDto().build();
        doThrow(new DuplicateExternalEntityException("Duplicate"))
                .when(importSupport).validateNotDuplicate(eq(dto.externalId()), any());

        // Act & Assert
        assertThrows(DuplicateExternalEntityException.class,
                () -> externalRideService.createExternalRide(dto));
        verify(rideRepository, never()).save(any());
    }
}

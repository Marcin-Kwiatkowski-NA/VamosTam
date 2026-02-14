package com.blablatwo.ride;

import com.blablatwo.domain.ExternalImportSupport;
import com.blablatwo.location.Location;
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

import java.util.List;
import java.util.Optional;

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

    private Location originLocation;
    private Location destinationLocation;
    private Location intermediateLocation;
    private UserAccount proxyUser;
    private RideResponseDto responseDto;

    @BeforeEach
    void setUp() {
        originLocation = anOriginLocation().build();
        destinationLocation = aDestinationLocation().build();
        intermediateLocation = aKrakowLocation().build();
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
        when(importSupport.resolveLocations(dto.originLocationName(), dto.destinationLocationName()))
                .thenReturn(new ExternalImportSupport.ResolvedLocations(originLocation, destinationLocation));

        // Act
        externalRideService.createExternalRide(dto);

        // Assert
        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository, times(2)).save(rideCaptor.capture());

        Ride savedRide = rideCaptor.getAllValues().get(1);
        assertEquals(2, savedRide.getStops().size());
        assertEquals(0, savedRide.getStops().get(0).getStopOrder());
        assertEquals(originLocation, savedRide.getStops().get(0).getLocation());
        assertNotNull(savedRide.getStops().get(0).getDepartureTime());
        assertEquals(1, savedRide.getStops().get(1).getStopOrder());
        assertEquals(destinationLocation, savedRide.getStops().get(1).getLocation());
        assertNull(savedRide.getStops().get(1).getDepartureTime());
    }

    @Test
    @DisplayName("Create external ride with intermediate stops creates all stops")
    void createExternalRide_withIntermediateStops_createsAllStops() {
        // Arrange
        ExternalRideCreationDto dto = anExternalRideWithStops().build();
        when(importSupport.resolveLocations(dto.originLocationName(), dto.destinationLocationName()))
                .thenReturn(new ExternalImportSupport.ResolvedLocations(originLocation, destinationLocation));
        when(importSupport.tryResolveLocationByName("Kraków")).thenReturn(Optional.of(intermediateLocation));

        // Act
        externalRideService.createExternalRide(dto);

        // Assert
        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository, times(2)).save(rideCaptor.capture());

        Ride savedRide = rideCaptor.getAllValues().get(1);
        assertEquals(3, savedRide.getStops().size());

        // Origin
        assertEquals(0, savedRide.getStops().get(0).getStopOrder());
        assertEquals(originLocation, savedRide.getStops().get(0).getLocation());
        assertNotNull(savedRide.getStops().get(0).getDepartureTime());

        // Intermediate — no departure time
        assertEquals(1, savedRide.getStops().get(1).getStopOrder());
        assertEquals(intermediateLocation, savedRide.getStops().get(1).getLocation());
        assertNull(savedRide.getStops().get(1).getDepartureTime());

        // Destination
        assertEquals(2, savedRide.getStops().get(2).getStopOrder());
        assertEquals(destinationLocation, savedRide.getStops().get(2).getLocation());
        assertNull(savedRide.getStops().get(2).getDepartureTime());
    }

    @Test
    @DisplayName("Unresolvable intermediate stops are skipped, ride still created")
    void createExternalRide_withUnresolvableIntermediateStop_skipsAndCreatesRide() {
        // Arrange — two intermediates: Kraków resolves, "Nowhereville" does not
        ExternalRideCreationDto dto = anExternalRideCreationDto()
                .intermediateStopLocationNames(List.of("Kraków", "Nowhereville"))
                .build();
        when(importSupport.resolveLocations(dto.originLocationName(), dto.destinationLocationName()))
                .thenReturn(new ExternalImportSupport.ResolvedLocations(originLocation, destinationLocation));
        when(importSupport.tryResolveLocationByName("Kraków")).thenReturn(Optional.of(intermediateLocation));
        when(importSupport.tryResolveLocationByName("Nowhereville")).thenReturn(Optional.empty());

        // Act
        externalRideService.createExternalRide(dto);

        // Assert — ride created with 3 stops (origin + Kraków + destination), Nowhereville skipped
        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository, times(2)).save(rideCaptor.capture());

        Ride savedRide = rideCaptor.getAllValues().get(1);
        assertEquals(3, savedRide.getStops().size());

        assertEquals(originLocation, savedRide.getStops().get(0).getLocation());
        assertEquals(0, savedRide.getStops().get(0).getStopOrder());

        assertEquals(intermediateLocation, savedRide.getStops().get(1).getLocation());
        assertEquals(1, savedRide.getStops().get(1).getStopOrder());

        assertEquals(destinationLocation, savedRide.getStops().get(2).getLocation());
        assertEquals(2, savedRide.getStops().get(2).getStopOrder());

        verify(metaRepository).save(any(RideExternalMeta.class));
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

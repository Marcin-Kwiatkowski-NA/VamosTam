package com.blablatwo.ride;

import com.blablatwo.domain.ExternalImportSupport;
import com.blablatwo.domain.Status;
import com.blablatwo.location.Location;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.ride.external.ExternalRideService;
import com.blablatwo.user.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

@Service
public class ExternalRideServiceImpl implements ExternalRideService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalRideServiceImpl.class);

    private final RideRepository rideRepository;
    private final RideExternalMetaRepository metaRepository;
    private final ExternalImportSupport importSupport;
    private final RideMapper rideMapper;
    private final RideResponseEnricher rideResponseEnricher;

    public ExternalRideServiceImpl(RideRepository rideRepository,
                                   RideExternalMetaRepository metaRepository,
                                   ExternalImportSupport importSupport,
                                   RideMapper rideMapper,
                                   RideResponseEnricher rideResponseEnricher) {
        this.rideRepository = rideRepository;
        this.metaRepository = metaRepository;
        this.importSupport = importSupport;
        this.rideMapper = rideMapper;
        this.rideResponseEnricher = rideResponseEnricher;
    }

    @Override
    @Transactional
    public RideResponseDto createExternalRide(com.blablatwo.ride.dto.ExternalRideCreationDto dto) {
        importSupport.validateNotDuplicate(dto.externalId(), metaRepository::existsByExternalId);

        var locations = importSupport.resolveLocations(dto.originLocationName(), dto.destinationLocationName());
        UserAccount proxy = importSupport.resolveProxyUser();

        Ride ride = Ride.builder()
                .driver(proxy)
                .departureDate(dto.departureDate())
                .departureTime(dto.departureTime())
                .isApproximate(dto.isApproximate())
                .source(RideSource.FACEBOOK)
                .totalSeats(dto.availableSeats())
                .pricePerSeat(dto.pricePerSeat())
                .status(Status.ACTIVE)
                .description(dto.description())
                .lastModified(Instant.now())
                .build();

        Ride saved = rideRepository.save(ride);

        var stops = new ArrayList<RideStop>();
        int order = 0;

        stops.add(RideStop.builder()
                .ride(saved).location(locations.origin()).stopOrder(order++)
                .departureTime(dto.departureDate().atTime(dto.departureTime()))
                .build());

        if (dto.intermediateStopLocationNames() != null) {
            for (String locationName : dto.intermediateStopLocationNames()) {
                Optional<Location> location = importSupport.tryResolveLocationByName(locationName);
                if (location.isPresent()) {
                    stops.add(RideStop.builder()
                            .ride(saved).location(location.get()).stopOrder(order++)
                            .departureTime(null)
                            .build());
                }
            }
        }

        stops.add(RideStop.builder()
                .ride(saved).location(locations.destination()).stopOrder(order)
                .departureTime(null)
                .build());

        saved.setStops(stops);
        rideRepository.save(saved);

        RideExternalMeta meta = RideExternalMeta.builder()
                .ride(saved)
                .sourceUrl(dto.sourceUrl())
                .externalId(dto.externalId())
                .rawContent(dto.rawContent())
                .phoneNumber(dto.phoneNumber())
                .authorName(dto.authorName())
                .build();

        metaRepository.save(meta);

        LOGGER.info("Created external ride with ID: {} from external source: {}",
                saved.getId(), dto.externalId());

        return rideResponseEnricher.enrich(saved, rideMapper.rideEntityToRideResponseDto(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByExternalId(String externalId) {
        return metaRepository.existsByExternalId(externalId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RideResponseDto> getByExternalId(String externalId) {
        return metaRepository.findByExternalId(externalId)
                .map(meta -> {
                    Ride ride = meta.getRide();
                    return rideResponseEnricher.enrich(ride, rideMapper.rideEntityToRideResponseDto(ride));
                });
    }
}

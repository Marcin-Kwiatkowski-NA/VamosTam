package com.blablatwo.ride;

import com.blablatwo.domain.ExternalImportSupport;
import com.blablatwo.domain.Status;
import com.blablatwo.domain.TimeSlot;
import com.blablatwo.ride.dto.ExternalRideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.ride.external.ExternalRideService;
import com.blablatwo.user.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    public RideResponseDto createExternalRide(ExternalRideCreationDto dto) {
        String contentHash = importSupport.computeHash(dto.rawContent());
        importSupport.validateAndDeduplicate(
                dto.externalId(), contentHash,
                metaRepository::existsByExternalId,
                metaRepository::existsByContentHash);

        String langCode = dto.lang() != null ? dto.lang().getCode() : null;
        var segment = importSupport.resolveSegment(dto.originCityName(), dto.destinationCityName(), langCode);
        UserAccount proxy = importSupport.resolveProxyUser();

        Ride ride = Ride.builder()
                .driver(proxy)
                .segment(segment)
                .timeSlot(new TimeSlot(dto.departureDate(), dto.departureTime(), dto.isApproximate()))
                .source(RideSource.FACEBOOK)
                .availableSeats(dto.availableSeats())
                .pricePerSeat(dto.pricePerSeat())
                .status(Status.ACTIVE)
                .description(dto.description())
                .lastModified(Instant.now())
                .build();

        Ride saved = rideRepository.save(ride);

        RideExternalMeta meta = RideExternalMeta.builder()
                .ride(saved)
                .sourceUrl(dto.sourceUrl())
                .externalId(dto.externalId())
                .rawContent(dto.rawContent())
                .contentHash(contentHash)
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

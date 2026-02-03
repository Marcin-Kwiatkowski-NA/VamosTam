package com.blablatwo.ride;

import com.blablatwo.city.City;
import com.blablatwo.city.CityResolutionService;
import com.blablatwo.config.DataInitializer;
import com.blablatwo.exceptions.DuplicateExternalRideException;
import com.blablatwo.exceptions.FacebookBotMissingException;
import com.blablatwo.ride.dto.ExternalRideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.ride.external.ExternalRideService;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class ExternalRideServiceImpl implements ExternalRideService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalRideServiceImpl.class);

    private final RideRepository rideRepository;
    private final RideExternalMetaRepository metaRepository;
    private final CityResolutionService cityResolutionService;
    private final UserAccountRepository userAccountRepository;
    private final RideMapper rideMapper;
    private final RideResponseEnricher rideResponseEnricher;

    public ExternalRideServiceImpl(RideRepository rideRepository,
                                   RideExternalMetaRepository metaRepository,
                                   CityResolutionService cityResolutionService,
                                   UserAccountRepository userAccountRepository,
                                   RideMapper rideMapper,
                                   RideResponseEnricher rideResponseEnricher) {
        this.rideRepository = rideRepository;
        this.metaRepository = metaRepository;
        this.cityResolutionService = cityResolutionService;
        this.userAccountRepository = userAccountRepository;
        this.rideMapper = rideMapper;
        this.rideResponseEnricher = rideResponseEnricher;
    }

    @Override
    @Transactional
    public RideResponseDto createExternalRide(ExternalRideCreationDto dto) {
        // 1. Content hash deduplication (same post across multiple groups)
        String contentHash = computeHash(dto.rawContent());
        if (contentHash != null && metaRepository.existsByContentHash(contentHash)) {
            throw new DuplicateExternalRideException("Duplicate content detected");
        }

        // 2. External ID deduplication (same post ID)
        if (metaRepository.existsByExternalId(dto.externalId())) {
            throw new DuplicateExternalRideException(dto.externalId());
        }

        // 3. Resolve cities via CityResolutionService (uses geocoding with lang strategy)
        String langCode = dto.lang() != null ? dto.lang().getCode() : null;
        City origin = cityResolutionService.resolveCityByName(dto.originCityName(), langCode);
        City destination = cityResolutionService.resolveCityByName(dto.destinationCityName(), langCode);

        // 4. Get Facebook bot user by email
        UserAccount proxy = userAccountRepository.findByEmail(DataInitializer.FACEBOOK_BOT_EMAIL)
                .orElseThrow(FacebookBotMissingException::new);

        // 5. Build and save Ride
        Ride ride = Ride.builder()
                .driver(proxy)
                .origin(origin)
                .destination(destination)
                .departureTime(dto.departureDate().atTime(dto.departureTime()))
                .isApproximate(dto.isApproximate())
                .source(RideSource.FACEBOOK)
                .availableSeats(dto.availableSeats())
                .pricePerSeat(dto.pricePerSeat())
                .rideStatus(RideStatus.OPEN)
                .description(dto.description())
                .lastModified(Instant.now())
                .build();

        Ride saved = rideRepository.save(ride);

        // 7. Save external metadata
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

    private String computeHash(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("SHA-256 algorithm not available", e);
            return null;
        }
    }

}

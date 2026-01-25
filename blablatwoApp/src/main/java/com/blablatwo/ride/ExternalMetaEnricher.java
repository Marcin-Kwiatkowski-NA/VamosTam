package com.blablatwo.ride;

import com.blablatwo.ride.dto.RideResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ExternalMetaEnricher {

    private final RideExternalMetaRepository metaRepository;

    public ExternalMetaEnricher(RideExternalMetaRepository metaRepository) {
        this.metaRepository = metaRepository;
    }

    /**
     * Enriches a list of RideResponseDto with external metadata.
     * For FACEBOOK rides: adds sourceUrl and overrides driver.phoneNumber if present in meta.
     */
    public List<RideResponseDto> enrich(List<RideResponseDto> rides) {
        Set<Long> facebookRideIds = rides.stream()
                .filter(r -> r.source() == RideSource.FACEBOOK)
                .map(RideResponseDto::id)
                .collect(Collectors.toSet());

        if (facebookRideIds.isEmpty()) {
            return rides;
        }

        Map<Long, RideExternalMeta> metaByRideId = metaRepository
                .findAllByRideIdIn(facebookRideIds)
                .stream()
                .collect(Collectors.toMap(RideExternalMeta::getRideId, Function.identity()));

        return rides.stream()
                .map(ride -> enrichSingle(ride, metaByRideId.get(ride.id())))
                .toList();
    }

    /**
     * Enriches a single RideResponseDto with external metadata.
     */
    public RideResponseDto enrich(RideResponseDto ride) {
        if (ride.source() != RideSource.FACEBOOK) {
            return ride;
        }
        RideExternalMeta meta = metaRepository.findById(ride.id()).orElse(null);
        return enrichSingle(ride, meta);
    }

    private RideResponseDto enrichSingle(RideResponseDto ride, RideExternalMeta meta) {
        if (meta == null) {
            return ride;
        }

        var builder = ride.toBuilder()
                .sourceUrl(meta.getSourceUrl());

        var driverBuilder = ride.driver().toBuilder();
        boolean driverModified = false;

        if (meta.getPhoneNumber() != null && !meta.getPhoneNumber().isBlank()) {
            driverBuilder.phoneNumber(meta.getPhoneNumber());
            driverModified = true;
        }

        if (meta.getAuthorName() != null && !meta.getAuthorName().isBlank()) {
            driverBuilder.name(meta.getAuthorName());
            driverModified = true;
        }

        if (driverModified) {
            builder.driver(driverBuilder.build());
        }

        return builder.build();
    }
}
package com.blablatwo.ride;

import com.blablatwo.domain.ResponseEnricher;
import com.blablatwo.dto.ContactMethodDto;
import com.blablatwo.dto.UserCardDto;
import com.blablatwo.ride.dto.RideResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RideResponseEnricher {

    private final RideExternalMetaRepository metaRepository;
    private final ResponseEnricher responseEnricher;

    public RideResponseEnricher(RideExternalMetaRepository metaRepository,
                                 ResponseEnricher responseEnricher) {
        this.metaRepository = metaRepository;
        this.responseEnricher = responseEnricher;
    }

    public List<RideResponseDto> enrich(List<Ride> rides, List<RideResponseDto> dtos) {
        return responseEnricher.enrich(rides, dtos,
                r -> r.getDriver().getId(),
                this::fetchAllMeta,
                this::assembleDto);
    }

    public RideResponseDto enrich(Ride ride, RideResponseDto dto) {
        return responseEnricher.enrich(ride, dto,
                r -> r.getDriver().getId(),
                this::fetchSingleMeta,
                this::assembleDto);
    }

    private Map<Long, RideExternalMeta> fetchAllMeta(Set<Long> rideIds) {
        return metaRepository.findAllByIdIn(rideIds)
                .stream()
                .collect(Collectors.toMap(RideExternalMeta::getRideId, Function.identity()));
    }

    private Optional<RideExternalMeta> fetchSingleMeta(Long rideId) {
        return metaRepository.findById(rideId);
    }

    private RideResponseDto assembleDto(RideResponseDto dto, UserCardDto userCard,
                                         List<ContactMethodDto> contactMethods) {
        return dto.toBuilder()
                .driver(userCard)
                .contactMethods(contactMethods)
                .build();
    }
}

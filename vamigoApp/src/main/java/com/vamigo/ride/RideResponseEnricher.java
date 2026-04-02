package com.vamigo.ride;

import com.vamigo.domain.ResponseEnricher;
import com.vamigo.dto.ContactMethodDto;
import com.vamigo.dto.UserCardDto;
import com.vamigo.ride.dto.RideResponseDto;
import com.vamigo.vehicle.LicensePlateMasker;
import com.vamigo.vehicle.VehiclePhotoUrlResolver;
import com.vamigo.vehicle.VehicleResponseDto;
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
    private final VehiclePhotoUrlResolver vehiclePhotoUrlResolver;

    public RideResponseEnricher(RideExternalMetaRepository metaRepository,
                                 ResponseEnricher responseEnricher,
                                 VehiclePhotoUrlResolver vehiclePhotoUrlResolver) {
        this.metaRepository = metaRepository;
        this.responseEnricher = responseEnricher;
        this.vehiclePhotoUrlResolver = vehiclePhotoUrlResolver;
    }

    public List<RideResponseDto> enrich(List<Ride> rides, List<RideResponseDto> dtos) {
        List<RideResponseDto> enriched = responseEnricher.enrich(rides, dtos,
                r -> r.getDriver().getId(),
                this::fetchAllMeta,
                this::assembleDto);
        // Post-process vehicle photo URL and mask plate on each ride
        List<RideResponseDto> result = new java.util.ArrayList<>(enriched.size());
        for (int i = 0; i < enriched.size(); i++) {
            result.add(postProcessVehicle(rides.get(i), enriched.get(i)));
        }
        return result;
    }

    public RideResponseDto enrich(Ride ride, RideResponseDto dto) {
        RideResponseDto enriched = responseEnricher.enrich(ride, dto,
                r -> r.getDriver().getId(),
                this::fetchSingleMeta,
                this::assembleDto);
        return postProcessVehicle(ride, enriched);
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

    private RideResponseDto postProcessVehicle(Ride ride, RideResponseDto dto) {
        if (ride.getVehicle() == null) return dto;
        VehicleResponseDto maskedVehicle = dto.vehicle().toBuilder()
                .photoUrl(vehiclePhotoUrlResolver.resolve(ride.getVehicle()))
                .licensePlate(LicensePlateMasker.mask(ride.getVehicle().getLicensePlate()))
                .build();
        return dto.toBuilder().vehicle(maskedVehicle).build();
    }
}

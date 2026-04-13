package com.vamigo.ride;

import com.vamigo.domain.ResponseEnricher;
import com.vamigo.dto.ContactMethodDto;
import com.vamigo.dto.UserCardDto;
import com.vamigo.ride.dto.RideListDto;
import com.vamigo.ride.dto.RideResponseDto;
import com.vamigo.user.AccountType;
import com.vamigo.user.CarrierProfile;
import com.vamigo.user.CarrierProfileRepository;
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
    private final CarrierProfileRepository carrierProfileRepository;

    public RideResponseEnricher(RideExternalMetaRepository metaRepository,
                                 ResponseEnricher responseEnricher,
                                 VehiclePhotoUrlResolver vehiclePhotoUrlResolver,
                                 CarrierProfileRepository carrierProfileRepository) {
        this.metaRepository = metaRepository;
        this.responseEnricher = responseEnricher;
        this.vehiclePhotoUrlResolver = vehiclePhotoUrlResolver;
        this.carrierProfileRepository = carrierProfileRepository;
    }

    public List<RideResponseDto> enrich(List<Ride> rides, List<RideResponseDto> dtos) {
        List<RideResponseDto> enriched = responseEnricher.enrich(rides, dtos,
                r -> r.getDriver().getId(),
                this::fetchAllMeta,
                this::assembleDto);
        // Post-process vehicle photo URL, mask plate, and resolve bookingEnabled
        Set<Long> carrierDriverIds = enriched.stream()
                .filter(dto -> dto.source() == RideSource.INTERNAL)
                .filter(dto -> dto.driver() != null && dto.driver().accountType() == AccountType.CARRIER)
                .map(dto -> dto.driver().id())
                .collect(Collectors.toSet());
        Map<Long, CarrierProfile> carrierProfiles = carrierDriverIds.isEmpty()
                ? Map.of()
                : carrierProfileRepository.findAllById(carrierDriverIds).stream()
                        .collect(Collectors.toMap(CarrierProfile::getId, Function.identity()));

        List<RideResponseDto> result = new java.util.ArrayList<>(enriched.size());
        for (int i = 0; i < enriched.size(); i++) {
            RideResponseDto processed = postProcessVehicle(rides.get(i), enriched.get(i));
            processed = resolveBookingEnabled(rides.get(i), processed, carrierProfiles);
            result.add(processed);
        }
        return result;
    }

    public RideResponseDto enrich(Ride ride, RideResponseDto dto) {
        RideResponseDto enriched = responseEnricher.enrich(ride, dto,
                r -> r.getDriver().getId(),
                this::fetchSingleMeta,
                this::assembleDto);
        RideResponseDto processed = postProcessVehicle(ride, enriched);
        return resolveBookingEnabled(ride, processed);
    }

    public List<RideListDto> enrichList(List<Ride> rides, List<RideListDto> dtos) {
        List<RideListDto> enriched = responseEnricher.enrichForList(rides, dtos,
                r -> r.getDriver().getId(),
                this::fetchAllMeta,
                this::assembleListDto);
        return resolveBookingEnabledForList(rides, enriched);
    }

    public RideListDto enrichList(Ride ride, RideListDto dto) {
        RideListDto enriched = responseEnricher.enrichForList(ride, dto,
                r -> r.getDriver().getId(),
                this::fetchSingleMeta,
                this::assembleListDto);
        return resolveBookingEnabledForList(ride, enriched);
    }

    private RideListDto assembleListDto(RideListDto dto, UserCardDto userCard) {
        return dto.toBuilder().driver(userCard).build();
    }

    private List<RideListDto> resolveBookingEnabledForList(List<Ride> rides, List<RideListDto> dtos) {
        Set<Long> carrierDriverIds = dtos.stream()
                .filter(dto -> dto.source() == RideSource.INTERNAL)
                .filter(dto -> dto.driver() != null && dto.driver().accountType() == AccountType.CARRIER)
                .map(dto -> dto.driver().id())
                .collect(Collectors.toSet());
        Map<Long, CarrierProfile> carrierProfiles = carrierDriverIds.isEmpty()
                ? Map.of()
                : carrierProfileRepository.findAllById(carrierDriverIds).stream()
                        .collect(Collectors.toMap(CarrierProfile::getId, Function.identity()));

        List<RideListDto> result = new java.util.ArrayList<>(dtos.size());
        for (int i = 0; i < dtos.size(); i++) {
            result.add(resolveBookingEnabledForList(rides.get(i), dtos.get(i), carrierProfiles));
        }
        return result;
    }

    private RideListDto resolveBookingEnabledForList(Ride ride, RideListDto dto) {
        if (ride.getSource() != RideSource.INTERNAL) {
            return dto.toBuilder().bookingEnabled(false).build();
        }
        if (dto.driver() != null && dto.driver().accountType() == AccountType.CARRIER) {
            boolean enabled = carrierProfileRepository.findById(dto.driver().id())
                    .map(CarrierProfile::isBookingEnabled)
                    .orElse(false);
            return dto.toBuilder().bookingEnabled(enabled).build();
        }
        return dto;
    }

    private RideListDto resolveBookingEnabledForList(Ride ride, RideListDto dto,
                                                      Map<Long, CarrierProfile> carrierProfiles) {
        if (ride.getSource() != RideSource.INTERNAL) {
            return dto.toBuilder().bookingEnabled(false).build();
        }
        if (dto.driver() != null && dto.driver().accountType() == AccountType.CARRIER) {
            CarrierProfile profile = carrierProfiles.get(dto.driver().id());
            boolean enabled = profile != null && profile.isBookingEnabled();
            return dto.toBuilder().bookingEnabled(enabled).build();
        }
        return dto;
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

    private RideResponseDto resolveBookingEnabled(Ride ride, RideResponseDto dto) {
        if (ride.getSource() != RideSource.INTERNAL) {
            return dto.toBuilder().bookingEnabled(false).build();
        }
        if (dto.driver() != null && dto.driver().accountType() == AccountType.CARRIER) {
            boolean enabled = carrierProfileRepository.findById(dto.driver().id())
                    .map(CarrierProfile::isBookingEnabled)
                    .orElse(false);
            return dto.toBuilder().bookingEnabled(enabled).build();
        }
        return dto;
    }

    private RideResponseDto resolveBookingEnabled(Ride ride, RideResponseDto dto,
                                                   Map<Long, CarrierProfile> carrierProfiles) {
        if (ride.getSource() != RideSource.INTERNAL) {
            return dto.toBuilder().bookingEnabled(false).build();
        }
        if (dto.driver() != null && dto.driver().accountType() == AccountType.CARRIER) {
            CarrierProfile profile = carrierProfiles.get(dto.driver().id());
            boolean enabled = profile != null && profile.isBookingEnabled();
            return dto.toBuilder().bookingEnabled(enabled).build();
        }
        return dto;
    }
}

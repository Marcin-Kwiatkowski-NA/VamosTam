package com.blablatwo.domain;

import com.blablatwo.dto.ContactMethodDto;
import com.blablatwo.dto.ContactMethodFactory;
import com.blablatwo.dto.UserCardDto;
import com.blablatwo.ride.RideSource;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import com.blablatwo.user.UserProfile;
import com.blablatwo.user.UserProfileRepository;
import com.blablatwo.user.UserStats;
import com.blablatwo.vehicle.Vehicle;
import com.blablatwo.vehicle.VehicleMapper;
import com.blablatwo.vehicle.VehicleRepository;
import com.blablatwo.vehicle.VehicleResponseDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class ResponseEnricher {

    private final UserProfileRepository userProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;
    private final ContactMethodFactory contactMethodFactory;
    private final PersonDisplayNameResolver displayNameResolver;

    public ResponseEnricher(UserProfileRepository userProfileRepository,
                            UserAccountRepository userAccountRepository,
                            VehicleRepository vehicleRepository,
                            VehicleMapper vehicleMapper,
                            ContactMethodFactory contactMethodFactory,
                            PersonDisplayNameResolver displayNameResolver) {
        this.userProfileRepository = userProfileRepository;
        this.userAccountRepository = userAccountRepository;
        this.vehicleRepository = vehicleRepository;
        this.vehicleMapper = vehicleMapper;
        this.contactMethodFactory = contactMethodFactory;
        this.displayNameResolver = displayNameResolver;
    }

    @FunctionalInterface
    public interface DtoAssembler<D> {
        D assemble(D dto, UserCardDto userCard, List<ContactMethodDto> contactMethods);
    }

    public <E extends AbstractTrip, D, M extends AbstractExternalMeta> List<D> enrich(
            List<E> entities, List<D> dtos,
            Function<E, Long> personId,
            Function<Set<Long>, Map<Long, M>> fetchAllMeta,
            DtoAssembler<D> assembler) {

        if (entities.size() != dtos.size()) {
            throw new IllegalArgumentException("Entities and DTOs lists must have the same size");
        }

        Set<Long> facebookIds = entities.stream()
                .filter(e -> e.getSource() == RideSource.FACEBOOK)
                .map(AbstractTrip::getId)
                .collect(Collectors.toSet());

        Map<Long, M> metaById = facebookIds.isEmpty()
                ? Map.of()
                : fetchAllMeta.apply(facebookIds);

        Set<Long> internalPersonIds = entities.stream()
                .filter(e -> e.getSource() == RideSource.INTERNAL)
                .map(personId)
                .collect(Collectors.toSet());

        Map<Long, UserProfile> profilesById = internalPersonIds.isEmpty()
                ? Map.of()
                : StreamSupport.stream(userProfileRepository.findAllById(internalPersonIds).spliterator(), false)
                        .collect(Collectors.toMap(UserProfile::getId, Function.identity()));

        Map<Long, UserAccount> accountsById = internalPersonIds.isEmpty()
                ? Map.of()
                : userAccountRepository.findAllById(internalPersonIds).stream()
                        .collect(Collectors.toMap(UserAccount::getId, Function.identity()));

        Map<Long, List<VehicleResponseDto>> vehiclesByOwnerId = internalPersonIds.isEmpty()
                ? Map.of()
                : vehicleRepository.findByOwnerIdIn(internalPersonIds).stream()
                        .collect(Collectors.groupingBy(
                                v -> v.getOwner().getId(),
                                Collectors.mapping(vehicleMapper::vehicleEntityToVehicleResponseDto, Collectors.toList())));

        List<D> result = new ArrayList<>(entities.size());
        for (int i = 0; i < entities.size(); i++) {
            E entity = entities.get(i);
            D dto = dtos.get(i);
            M meta = metaById.get(entity.getId());
            Long pid = personId.apply(entity);
            UserProfile profile = entity.getSource() == RideSource.INTERNAL
                    ? profilesById.get(pid)
                    : null;
            UserAccount account = entity.getSource() == RideSource.INTERNAL
                    ? accountsById.get(pid)
                    : null;
            List<VehicleResponseDto> vehicles = entity.getSource() == RideSource.INTERNAL
                    ? vehiclesByOwnerId.getOrDefault(pid, List.of())
                    : List.of();
            result.add(enrichSingle(entity, dto, personId, meta, profile, account, vehicles, assembler));
        }
        return result;
    }

    public <E extends AbstractTrip, D, M extends AbstractExternalMeta> D enrich(
            E entity, D dto,
            Function<E, Long> personId,
            Function<Long, Optional<M>> fetchSingleMeta,
            DtoAssembler<D> assembler) {

        M meta = null;
        UserProfile profile = null;
        UserAccount account = null;
        List<VehicleResponseDto> vehicles = List.of();

        if (entity.getSource() == RideSource.FACEBOOK) {
            meta = fetchSingleMeta.apply(entity.getId()).orElse(null);
        } else {
            Long pid = personId.apply(entity);
            profile = userProfileRepository.findById(pid).orElse(null);
            account = userAccountRepository.findById(pid).orElse(null);
            vehicles = vehicleRepository.findByOwnerId(pid).stream()
                    .map(vehicleMapper::vehicleEntityToVehicleResponseDto)
                    .toList();
        }
        return enrichSingle(entity, dto, personId, meta, profile, account, vehicles, assembler);
    }

    private <E extends AbstractTrip, D, M extends AbstractExternalMeta> D enrichSingle(
            E entity, D dto,
            Function<E, Long> personId,
            M meta, UserProfile profile, UserAccount account,
            List<VehicleResponseDto> vehicles,
            DtoAssembler<D> assembler) {

        UserCardDto userCard = buildUserCard(entity, personId, meta, profile, account, vehicles);
        String phoneNumber = profile != null ? profile.getPhoneNumber() : null;
        List<ContactMethodDto> contactMethods = contactMethodFactory.buildContactMethods(
                entity, meta, phoneNumber);
        return assembler.assemble(dto, userCard, contactMethods);
    }

    private <E extends AbstractTrip, M extends AbstractExternalMeta> UserCardDto buildUserCard(
            E entity, Function<E, Long> personId, M meta, UserProfile profile,
            UserAccount account, List<VehicleResponseDto> vehicles) {

        Long id = personId.apply(entity);
        String name;

        if (entity.getSource() == RideSource.FACEBOOK) {
            name = displayNameResolver.resolveExternal(
                    meta != null ? meta.getAuthorName() : null, entity.getId());
            return new UserCardDto(id, name, null, null,
                    null, null, false, false, 0, 0, 0, List.of());
        }

        name = displayNameResolver.resolveInternal(profile, id);

        String avatarUrl = null;
        String bio = null;
        boolean emailVerified = false;
        boolean phoneVerified = false;
        int ridesGiven = 0;
        int ridesTaken = 0;
        int ratingCount = 0;
        Double rating = null;

        if (profile != null) {
            avatarUrl = profile.getAvatarUrl();
            bio = profile.getBio();

            UserStats stats = profile.getStats();
            if (stats != null) {
                ridesGiven = stats.getRidesGiven();
                ridesTaken = stats.getRidesTaken();
                ratingCount = stats.getRatingCount();
                if (ratingCount > 0) {
                    rating = BigDecimal.valueOf(stats.getRatingSum())
                            .divide(BigDecimal.valueOf(ratingCount), 2, RoundingMode.HALF_UP)
                            .doubleValue();
                }
            }
        }

        if (account != null) {
            emailVerified = account.getEmailVerifiedAt() != null;
            phoneVerified = account.getPhoneVerifiedAt() != null;
        }

        return new UserCardDto(id, name, rating, ridesGiven + ridesTaken,
                avatarUrl, bio, emailVerified, phoneVerified,
                ridesGiven, ridesTaken, ratingCount, vehicles);
    }
}

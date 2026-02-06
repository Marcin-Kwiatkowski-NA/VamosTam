package com.blablatwo.domain;

import com.blablatwo.dto.ContactMethodDto;
import com.blablatwo.dto.ContactMethodFactory;
import com.blablatwo.dto.UserCardDto;
import com.blablatwo.ride.RideSource;
import com.blablatwo.user.UserProfile;
import com.blablatwo.user.UserProfileRepository;
import org.springframework.stereotype.Component;

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
    private final ContactMethodFactory contactMethodFactory;
    private final PersonDisplayNameResolver displayNameResolver;

    public ResponseEnricher(UserProfileRepository userProfileRepository,
                            ContactMethodFactory contactMethodFactory,
                            PersonDisplayNameResolver displayNameResolver) {
        this.userProfileRepository = userProfileRepository;
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

        List<D> result = new ArrayList<>(entities.size());
        for (int i = 0; i < entities.size(); i++) {
            E entity = entities.get(i);
            D dto = dtos.get(i);
            M meta = metaById.get(entity.getId());
            UserProfile profile = entity.getSource() == RideSource.INTERNAL
                    ? profilesById.get(personId.apply(entity))
                    : null;
            result.add(enrichSingle(entity, dto, personId, meta, profile, assembler));
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

        if (entity.getSource() == RideSource.FACEBOOK) {
            meta = fetchSingleMeta.apply(entity.getId()).orElse(null);
        } else {
            profile = userProfileRepository.findById(personId.apply(entity)).orElse(null);
        }
        return enrichSingle(entity, dto, personId, meta, profile, assembler);
    }

    private <E extends AbstractTrip, D, M extends AbstractExternalMeta> D enrichSingle(
            E entity, D dto,
            Function<E, Long> personId,
            M meta, UserProfile profile,
            DtoAssembler<D> assembler) {

        UserCardDto userCard = buildUserCard(entity, personId, meta, profile);
        String phoneNumber = profile != null ? profile.getPhoneNumber() : null;
        List<ContactMethodDto> contactMethods = contactMethodFactory.buildContactMethods(
                entity, meta, phoneNumber);
        return assembler.assemble(dto, userCard, contactMethods);
    }

    private <E extends AbstractTrip, M extends AbstractExternalMeta> UserCardDto buildUserCard(
            E entity, Function<E, Long> personId, M meta, UserProfile profile) {

        Long id = personId.apply(entity);
        String name;

        if (entity.getSource() == RideSource.FACEBOOK) {
            name = displayNameResolver.resolveExternal(
                    meta != null ? meta.getAuthorName() : null, entity.getId());
        } else {
            name = displayNameResolver.resolveInternal(profile, id);
        }

        return new UserCardDto(id, name, null, null);
    }
}

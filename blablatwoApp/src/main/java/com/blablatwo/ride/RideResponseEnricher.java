package com.blablatwo.ride;

import com.blablatwo.ride.dto.ContactMethodDto;
import com.blablatwo.ride.dto.ContactType;
import com.blablatwo.ride.dto.DriverDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.user.UserProfile;
import com.blablatwo.user.UserProfileRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class RideResponseEnricher {

    private final RideExternalMetaRepository metaRepository;
    private final UserProfileRepository userProfileRepository;

    public RideResponseEnricher(RideExternalMetaRepository metaRepository,
                                 UserProfileRepository userProfileRepository) {
        this.metaRepository = metaRepository;
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * Enriches a list of RideResponseDto with driver info and contact methods.
     * For INTERNAL rides: driver from Ride.driver, contact from driver.phoneNumber
     * For FACEBOOK rides: driver from RideExternalMeta, contact via sourceUrl link
     */
    public List<RideResponseDto> enrich(List<Ride> rides, List<RideResponseDto> dtos) {
        if (rides.size() != dtos.size()) {
            throw new IllegalArgumentException("Rides and DTOs lists must have the same size");
        }

        // Batch load external meta for FACEBOOK rides
        Set<Long> facebookRideIds = rides.stream()
                .filter(r -> r.getSource() == RideSource.FACEBOOK)
                .map(Ride::getId)
                .collect(Collectors.toSet());

        Map<Long, RideExternalMeta> metaByRideId = facebookRideIds.isEmpty()
                ? Map.of()
                : metaRepository.findAllByRideIdIn(facebookRideIds)
                        .stream()
                        .collect(Collectors.toMap(RideExternalMeta::getRideId, Function.identity()));

        // Batch load profiles for INTERNAL rides
        Set<Long> internalDriverIds = rides.stream()
                .filter(r -> r.getSource() == RideSource.INTERNAL)
                .map(r -> r.getDriver().getId())
                .collect(Collectors.toSet());

        Map<Long, UserProfile> profilesById = internalDriverIds.isEmpty()
                ? Map.of()
                : StreamSupport.stream(userProfileRepository.findAllById(internalDriverIds).spliterator(), false)
                        .collect(Collectors.toMap(UserProfile::getId, Function.identity()));

        List<RideResponseDto> result = new ArrayList<>(rides.size());
        for (int i = 0; i < rides.size(); i++) {
            Ride ride = rides.get(i);
            RideResponseDto dto = dtos.get(i);
            RideExternalMeta meta = metaByRideId.get(ride.getId());
            UserProfile profile = ride.getSource() == RideSource.INTERNAL
                    ? profilesById.get(ride.getDriver().getId())
                    : null;
            result.add(enrichSingle(ride, dto, meta, profile));
        }
        return result;
    }

    /**
     * Enriches a single RideResponseDto with driver info and contact methods.
     */
    public RideResponseDto enrich(Ride ride, RideResponseDto dto) {
        RideExternalMeta meta = null;
        UserProfile profile = null;

        if (ride.getSource() == RideSource.FACEBOOK) {
            meta = metaRepository.findById(ride.getId()).orElse(null);
        } else {
            profile = userProfileRepository.findById(ride.getDriver().getId()).orElse(null);
        }
        return enrichSingle(ride, dto, meta, profile);
    }

    private RideResponseDto enrichSingle(Ride ride, RideResponseDto dto, RideExternalMeta meta, UserProfile profile) {
        DriverDto driver = buildDriver(ride, meta, profile);
        List<ContactMethodDto> contactMethods = buildContactMethods(ride, meta, profile);

        return dto.toBuilder()
                .driver(driver)
                .contactMethods(contactMethods)
                .build();
    }

    private DriverDto buildDriver(Ride ride, RideExternalMeta meta, UserProfile profile) {
        Long id = ride.getDriver().getId();
        String name;

        if (ride.getSource() == RideSource.FACEBOOK) {
            Objects.requireNonNull(meta, "RideExternalMeta required for Facebook ride: rideId=" + ride.getId());
            name = Objects.requireNonNull(meta.getAuthorName(),
                    "Author name required for Facebook ride: rideId=" + ride.getId());
        } else {
            if (profile == null) {
                throw new IllegalStateException("UserProfile missing for driver: " + id);
            }
            String displayName = profile.getDisplayName();
            if (displayName == null || displayName.isBlank()) {
                throw new IllegalStateException("displayName blank for driver: " + id);
            }
            name = displayName;
        }

        return new DriverDto(id, name, null, null);
    }

    private List<ContactMethodDto> buildContactMethods(Ride ride, RideExternalMeta meta, UserProfile profile) {
        List<ContactMethodDto> contacts = new ArrayList<>();

        if (ride.getSource() == RideSource.FACEBOOK) {
            if (meta != null && meta.getSourceUrl() != null) {
                contacts.add(new ContactMethodDto(ContactType.FACEBOOK_LINK, meta.getSourceUrl()));
            }
            if (meta != null && meta.getPhoneNumber() != null && !meta.getPhoneNumber().isBlank()) {
                contacts.add(new ContactMethodDto(ContactType.PHONE, meta.getPhoneNumber()));
            }
        } else {
            if (profile != null && profile.getPhoneNumber() != null && !profile.getPhoneNumber().isBlank()) {
                contacts.add(new ContactMethodDto(ContactType.PHONE, profile.getPhoneNumber()));
            }
        }

        return contacts;
    }
}

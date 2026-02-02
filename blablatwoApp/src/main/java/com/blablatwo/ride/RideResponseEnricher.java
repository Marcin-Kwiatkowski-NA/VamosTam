package com.blablatwo.ride;

import com.blablatwo.ride.dto.ContactMethodDto;
import com.blablatwo.ride.dto.ContactType;
import com.blablatwo.ride.dto.DriverDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.traveler.FacebookProxyConstants;
import com.blablatwo.traveler.Traveler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RideResponseEnricher {

    private final RideExternalMetaRepository metaRepository;

    public RideResponseEnricher(RideExternalMetaRepository metaRepository) {
        this.metaRepository = metaRepository;
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

        Set<Long> facebookRideIds = rides.stream()
                .filter(r -> r.getSource() == RideSource.FACEBOOK)
                .map(Ride::getId)
                .collect(Collectors.toSet());

        Map<Long, RideExternalMeta> metaByRideId = facebookRideIds.isEmpty()
                ? Map.of()
                : metaRepository.findAllByRideIdIn(facebookRideIds)
                        .stream()
                        .collect(Collectors.toMap(RideExternalMeta::getRideId, Function.identity()));

        List<RideResponseDto> result = new ArrayList<>(rides.size());
        for (int i = 0; i < rides.size(); i++) {
            Ride ride = rides.get(i);
            RideResponseDto dto = dtos.get(i);
            RideExternalMeta meta = metaByRideId.get(ride.getId());
            result.add(enrichSingle(ride, dto, meta));
        }
        return result;
    }

    /**
     * Enriches a single RideResponseDto with driver info and contact methods.
     */
    public RideResponseDto enrich(Ride ride, RideResponseDto dto) {
        RideExternalMeta meta = null;
        if (ride.getSource() == RideSource.FACEBOOK) {
            meta = metaRepository.findById(ride.getId()).orElse(null);
        }
        return enrichSingle(ride, dto, meta);
    }

    private RideResponseDto enrichSingle(Ride ride, RideResponseDto dto, RideExternalMeta meta) {
        DriverDto driver = buildDriver(ride, meta);
        List<ContactMethodDto> contactMethods = buildContactMethods(ride, meta);

        return dto.toBuilder()
                .driver(driver)
                .contactMethods(contactMethods)
                .build();
    }

    private DriverDto buildDriver(Ride ride, RideExternalMeta meta) {
        Long id;
        String name;

        if (ride.getSource() == RideSource.FACEBOOK) {
            Objects.requireNonNull(meta, "RideExternalMeta required for Facebook ride: rideId=" + ride.getId());
            id = FacebookProxyConstants.FACEBOOK_PROXY_ID;
            name = Objects.requireNonNull(meta.getAuthorName(),
                    "Author name required for Facebook ride: rideId=" + ride.getId());
        } else {
            Traveler driver = Objects.requireNonNull(ride.getDriver(),
                    "Driver required for internal ride: rideId=" + ride.getId());
            id = Objects.requireNonNull(driver.getId(),
                    "Driver id required: rideId=" + ride.getId());
            name = Objects.requireNonNull(driver.getName(),
                    "Driver name required: rideId=" + ride.getId());
        }

        if (name.isBlank()) {
            throw new IllegalStateException("Driver name cannot be blank: rideId=" + ride.getId());
        }

        return new DriverDto(id, name, null, null);
    }

    private List<ContactMethodDto> buildContactMethods(Ride ride, RideExternalMeta meta) {
        List<ContactMethodDto> contacts = new ArrayList<>();

        if (ride.getSource() == RideSource.FACEBOOK) {
            // For Facebook rides, use the source URL as contact
            if (meta != null && meta.getSourceUrl() != null) {
                contacts.add(new ContactMethodDto(ContactType.FACEBOOK_LINK, meta.getSourceUrl()));
            }
            // Also add phone if available from meta
            if (meta != null && meta.getPhoneNumber() != null && !meta.getPhoneNumber().isBlank()) {
                contacts.add(new ContactMethodDto(ContactType.PHONE, meta.getPhoneNumber()));
            }
        } else {
            // For internal rides, use driver's phone number
            if (ride.getDriver() != null && ride.getDriver().getPhoneNumber() != null
                    && !ride.getDriver().getPhoneNumber().isBlank()) {
                contacts.add(new ContactMethodDto(ContactType.PHONE, ride.getDriver().getPhoneNumber()));
            }
        }

        return contacts;
    }
}

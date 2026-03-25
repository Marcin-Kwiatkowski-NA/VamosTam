package com.vamigo.location;

import com.vamigo.location.photon.PhotonClient;
import com.vamigo.location.photon.PhotonFeature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@EnableConfigurationProperties(LocationProperties.class)
public class LocationResolutionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocationResolutionService.class);
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final LocationRepository locationRepository;
    private final PhotonClient photonClient;
    private final LocationProperties locationProperties;

    public LocationResolutionService(LocationRepository locationRepository,
                                      PhotonClient photonClient,
                                      LocationProperties locationProperties) {
        this.locationRepository = locationRepository;
        this.photonClient = photonClient;
        this.locationProperties = locationProperties;
    }

    @Transactional
    public Location resolve(LocationRef ref) {
        Optional<Location> existing = locationRepository.findByOsmId(ref.osmId());
        if (existing.isPresent()) {
            LOGGER.debug("Found existing location by osmId: {}", ref.osmId());
            return existing.get();
        }

        LOGGER.info("Creating new location from ref: osmId={}, name={}, lang={}", ref.osmId(), ref.name(), ref.lang());

        LocationLang otherLang = ref.lang() == LocationLang.pl ? LocationLang.en : LocationLang.pl;
        ReversedFields otherFields = reverseByOsmId(ref.latitude(), ref.longitude(), ref.osmId(), otherLang)
                .orElse(new ReversedFields(ref.name(), ref.state()));

        String namePl = ref.lang() == LocationLang.pl ? ref.name() : otherFields.name();
        String nameEn = ref.lang() == LocationLang.en ? ref.name() : otherFields.name();
        String statePl = ref.lang() == LocationLang.pl ? ref.state() : otherFields.state();
        String stateEn = ref.lang() == LocationLang.en ? ref.state() : otherFields.state();

        Location location = Location.builder()
                .osmId(ref.osmId())
                .namePl(namePl)
                .nameEn(nameEn)
                .country(ref.country())
                .countryCode(ref.countryCode())
                .statePl(statePl)
                .stateEn(stateEn)
                .county(ref.county())
                .city(ref.city())
                .postCode(ref.postCode())
                .type(ref.type())
                .osmKey(ref.osmKey())
                .osmValue(ref.osmValue())
                .coordinates(GEOMETRY_FACTORY.createPoint(new Coordinate(ref.longitude(), ref.latitude())))
                .build();

        return locationRepository.save(location);
    }

    @Transactional
    public Location resolveByName(String name) {
        return tryResolveByName(name)
                .orElseThrow(() -> new NoSuchLocationException(
                        name == null || name.isBlank() ? "Location name is required" : name));
    }

    @Transactional
    public Optional<Location> tryResolveByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        Optional<Location> overridden = resolveOverride(name);
        if (overridden.isPresent()) {
            return overridden;
        }

        // External import sends Polish city names
        List<PhotonFeature> results = photonClient.search(name, 1, LocationLang.pl);
        if (results.isEmpty()) {
            LOGGER.warn("Could not resolve location by name: '{}'", name);
            return Optional.empty();
        }

        PhotonFeature feature = results.getFirst();
        Long osmId = feature.properties().osmId();

        Optional<Location> existing = locationRepository.findByOsmId(osmId);
        if (existing.isPresent()) {
            LOGGER.debug("Found existing location by osmId from Photon: {}", osmId);
            return existing;
        }

        String namePl = feature.properties().name();
        String statePl = feature.properties().state();

        var coords = feature.geometry().coordinates();
        double lon = coords.get(0);
        double lat = coords.get(1);

        ReversedFields enFields = reverseByOsmId(lat, lon, osmId, LocationLang.en)
                .orElse(new ReversedFields(namePl, statePl));

        Location location = buildLocationFromFeature(feature, namePl, enFields.name(), statePl, enFields.state());
        return Optional.of(locationRepository.save(location));
    }

    private Optional<Location> resolveOverride(String name) {
        Long osmId = locationProperties.overrides().get(name.trim().toLowerCase());
        if (osmId == null) {
            return Optional.empty();
        }
        Optional<Location> pinned = locationRepository.findByOsmId(osmId);
        if (pinned.isPresent()) {
            LOGGER.info("Resolved '{}' via pinned override to osmId={}", name, osmId);
        } else {
            LOGGER.warn("Pinned override for '{}' → osmId={} not found in database, falling back to Photon", name, osmId);
        }
        return pinned;
    }

    record ReversedFields(String name, String state) {}

    Optional<ReversedFields> reverseByOsmId(double lat, double lon, Long osmId, LocationLang lang) {
        try {
            List<PhotonFeature> features = photonClient.reverse(lat, lon, lang);
            return features.stream()
                    .filter(f -> osmId.equals(f.properties().osmId()))
                    .map(f -> new ReversedFields(f.properties().name(), f.properties().state()))
                    .findFirst();
        } catch (Exception e) {
            LOGGER.warn("Reverse geocode failed for lang={}: {}", lang, e.getMessage());
            return Optional.empty();
        }
    }

    private Location buildLocationFromFeature(PhotonFeature feature, String namePl, String nameEn,
                                               String statePl, String stateEn) {
        var props = feature.properties();
        var coords = feature.geometry().coordinates();
        double lon = coords.get(0);
        double lat = coords.get(1);

        return Location.builder()
                .osmId(props.osmId())
                .namePl(namePl)
                .nameEn(nameEn)
                .country(props.country())
                .countryCode(props.countrycode())
                .statePl(statePl)
                .stateEn(stateEn)
                .county(props.county())
                .city(props.city())
                .postCode(props.postcode())
                .type(props.type())
                .osmKey(props.osmKey())
                .osmValue(props.osmValue())
                .coordinates(GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat)))
                .build();
    }
}

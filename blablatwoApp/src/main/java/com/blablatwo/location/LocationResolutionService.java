package com.blablatwo.location;

import com.blablatwo.location.photon.PhotonClient;
import com.blablatwo.location.photon.PhotonFeature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class LocationResolutionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocationResolutionService.class);
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final LocationRepository locationRepository;
    private final PhotonClient photonClient;

    public LocationResolutionService(LocationRepository locationRepository,
                                      PhotonClient photonClient) {
        this.locationRepository = locationRepository;
        this.photonClient = photonClient;
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
        String otherName = reverseNameByOsmId(ref.latitude(), ref.longitude(), ref.osmId(), otherLang)
                .orElse(ref.name());

        String namePl = ref.lang() == LocationLang.pl ? ref.name() : otherName;
        String nameEn = ref.lang() == LocationLang.en ? ref.name() : otherName;

        Location location = Location.builder()
                .osmId(ref.osmId())
                .namePl(namePl)
                .nameEn(nameEn)
                .country(ref.country())
                .countryCode(ref.countryCode())
                .state(ref.state())
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

        var coords = feature.geometry().coordinates();
        double lon = coords.get(0);
        double lat = coords.get(1);

        String nameEn = reverseNameByOsmId(lat, lon, osmId, LocationLang.en)
                .orElse(namePl);

        Location location = buildLocationFromFeature(feature, namePl, nameEn);
        return Optional.of(locationRepository.save(location));
    }

    private Optional<String> reverseNameByOsmId(double lat, double lon, Long osmId, LocationLang lang) {
        try {
            List<PhotonFeature> features = photonClient.reverse(lat, lon, lang);
            return findNameByOsmId(features, osmId);
        } catch (Exception e) {
            LOGGER.warn("Reverse geocode failed for lang={}: {}", lang, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> findNameByOsmId(List<PhotonFeature> features, Long osmId) {
        return features.stream()
                .filter(f -> osmId.equals(f.properties().osmId()))
                .map(f -> f.properties().name())
                .findFirst();
    }

    private Location buildLocationFromFeature(PhotonFeature feature, String namePl, String nameEn) {
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
                .state(props.state())
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

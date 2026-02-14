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
    private final LocationMapper locationMapper;
    private final PhotonClient photonClient;

    public LocationResolutionService(LocationRepository locationRepository,
                                      LocationMapper locationMapper,
                                      PhotonClient photonClient) {
        this.locationRepository = locationRepository;
        this.locationMapper = locationMapper;
        this.photonClient = photonClient;
    }

    @Transactional
    public Location resolve(LocationRef ref) {
        Optional<Location> existing = locationRepository.findByOsmId(ref.osmId());
        if (existing.isPresent()) {
            LOGGER.debug("Found existing location by osmId: {}", ref.osmId());
            return existing.get();
        }

        LOGGER.info("Creating new location from ref: osmId={}, name={}", ref.osmId(), ref.name());
        Location location = locationMapper.locationRefToEntity(ref);
        return locationRepository.save(location);
    }

    @Transactional
    public Location resolveByName(String name) {
        if (name == null || name.isBlank()) {
            throw new NoSuchLocationException("Location name is required");
        }

        List<PhotonFeature> results = photonClient.search(name, 1);
        if (results.isEmpty()) {
            throw new NoSuchLocationException(name);
        }

        PhotonFeature feature = results.getFirst();
        Long osmId = feature.properties().osmId();

        Optional<Location> existing = locationRepository.findByOsmId(osmId);
        if (existing.isPresent()) {
            LOGGER.debug("Found existing location by osmId from Photon: {}", osmId);
            return existing.get();
        }

        Location location = mapFeatureToLocation(feature);
        return locationRepository.save(location);
    }

    private Location mapFeatureToLocation(PhotonFeature feature) {
        var props = feature.properties();
        var coords = feature.geometry().coordinates();
        double lon = coords.get(0);
        double lat = coords.get(1);

        return Location.builder()
                .osmId(props.osmId())
                .name(props.name())
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

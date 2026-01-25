package com.blablatwo.city;

import com.blablatwo.city.dto.PhotonFeature;
import com.blablatwo.city.dto.PhotonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Service
public class PhotonServiceImpl implements PhotonService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhotonServiceImpl.class);

    private final RestClient restClient;
    private final CityRepository cityRepository;

    public PhotonServiceImpl(CityRepository cityRepository,
                             @Value("${photon.url}") String photonUrl) {
        this.cityRepository = cityRepository;
        this.restClient = RestClient.builder()
                .baseUrl(photonUrl)
                .build();
    }

    @Override
    @Transactional
    public Optional<City> resolveCity(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return Optional.empty();
        }

        Optional<City> existingByName = cityRepository.findByNameIgnoreCase(cityName);
        if (existingByName.isPresent()) {
            LOGGER.debug("Found city by name: {}", cityName);
            return existingByName;
        }

        PhotonResponse response;
        try {
            LOGGER.debug("Calling Photon API for city: {}", cityName);

            // 2. Use the uriBuilder lambda.
            // This safely combines the baseUrl with "/api", adds params, and handles encoding.
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api")
                            .queryParam("q", cityName)
                            .queryParam("osm_tag", "place:city", "place:town", "place:village")
                            .queryParam("limit", 1)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(PhotonResponse.class);

        } catch (Exception e) {
            LOGGER.error("Failed to call Photon API for city: {}", cityName, e);
            return Optional.empty();
        }

        if (response == null || response.features() == null || response.features().isEmpty()) {
            LOGGER.debug("City not found in Photon: {}", cityName);
            return Optional.empty();
        }

        PhotonFeature feature = response.features().get(0);
        Long osmId = feature.properties().osmId();
        String resolvedName = feature.properties().name();

        if (osmId == null) {
            LOGGER.warn("Photon returned null osmId for city: {}", cityName);
            return Optional.empty();
        }

        Optional<City> existingByOsmId = cityRepository.findByOsmId(osmId);
        if (existingByOsmId.isPresent()) {
            LOGGER.debug("Found city by osmId: {} (searched for: {})", osmId, cityName);
            return existingByOsmId;
        }

        City newCity = City.builder()
                .osmId(osmId)
                .name(resolvedName)
                .build();

        City savedCity = cityRepository.save(newCity);
        LOGGER.info("Created new city from Photon: {} (osmId: {})", resolvedName, osmId);

        return Optional.of(savedCity);
    }
}

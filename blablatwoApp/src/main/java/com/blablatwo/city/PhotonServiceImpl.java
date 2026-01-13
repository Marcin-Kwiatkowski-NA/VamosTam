package com.blablatwo.city;

import com.blablatwo.city.dto.PhotonFeature;
import com.blablatwo.city.dto.PhotonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class PhotonServiceImpl implements PhotonService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhotonServiceImpl.class);

    private static final String PHOTON_URL =
            "http://photon.130.61.31.172.sslip.io/api?q=%s&osm_tag=place:city&osm_tag=place:town&osm_tag=place:village&limit=1";

    private final RestTemplate restTemplate;
    private final CityRepository cityRepository;

    public PhotonServiceImpl(CityRepository cityRepository) {
        this.restTemplate = new RestTemplate();
        this.cityRepository = cityRepository;
    }

    @Override
    @Transactional
    public Optional<City> resolveCity(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return Optional.empty();
        }

        // 1. First check if city exists by name (case-insensitive)
        Optional<City> existingByName = cityRepository.findByNameIgnoreCase(cityName);
        if (existingByName.isPresent()) {
            LOGGER.debug("Found city by name: {}", cityName);
            return existingByName;
        }

        // 2. Call Photon API
        String encodedCityName = URLEncoder.encode(cityName, StandardCharsets.UTF_8);
        String url = String.format(PHOTON_URL, encodedCityName);

        PhotonResponse response;
        try {
            response = restTemplate.getForObject(url, PhotonResponse.class);
        } catch (RestClientException e) {
            LOGGER.error("Failed to call Photon API for city: {}", cityName, e);
            return Optional.empty();
        }

        if (response == null || response.features() == null || response.features().isEmpty()) {
            LOGGER.debug("City not found in Photon: {}", cityName);
            return Optional.empty();
        }

        // 3. Extract osmId and name from response
        PhotonFeature feature = response.features().get(0);
        Long osmId = feature.properties().osmId();
        String resolvedName = feature.properties().name();

        if (osmId == null) {
            LOGGER.warn("Photon returned null osmId for city: {}", cityName);
            return Optional.empty();
        }

        // 4. Check if city exists by osmId (might have different name spelling)
        Optional<City> existingByOsmId = cityRepository.findByOsmId(osmId);
        if (existingByOsmId.isPresent()) {
            LOGGER.debug("Found city by osmId: {} (searched for: {})", osmId, cityName);
            return existingByOsmId;
        }

        // 5. Create new city
        City newCity = City.builder()
                .osmId(osmId)
                .name(resolvedName)
                .build();

        City savedCity = cityRepository.save(newCity);
        LOGGER.info("Created new city from Photon: {} (osmId: {})", resolvedName, osmId);

        return Optional.of(savedCity);
    }
}

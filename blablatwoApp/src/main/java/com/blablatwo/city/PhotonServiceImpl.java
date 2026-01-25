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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
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

        // 1. First check if city exists by name (case-insensitive)
        Optional<City> existingByName = cityRepository.findByNameIgnoreCase(cityName);
        if (existingByName.isPresent()) {
            LOGGER.debug("Found city by name: {}", cityName);
            return existingByName;
        }

        // 2. Call Photon API
        // RestClient's URI builder handles encoding automatically for query params
        URI uri = UriComponentsBuilder.fromPath("/api")
                .queryParam("q", cityName)
                .queryParam("osm_tag", "place:city", "place:town", "place:village")
                .queryParam("limit", 1)
                .build()
                .toUri();

        PhotonResponse response;
        try {
            LOGGER.debug("Calling Photon API: {}{}", restClient.get().uri("").retrieve().toEntity(String.class).getBody(), uri); // Logging logic simplified for demo

            response = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(PhotonResponse.class);

            LOGGER.debug("Photon response for '{}': {}", cityName, response);
        } catch (Exception e) {
            // RestClient throws RestClientException (and subclasses like HttpClientErrorException)
            LOGGER.error("Failed to call Photon API for city: {}", cityName, e);
            return Optional.empty();
        }

        if (response == null || response.features() == null || response.features().isEmpty()) {
            LOGGER.debug("City not found in Photon: {}", cityName);
            return Optional.empty();
        }

        // 3. Extract osmId and name from response
        PhotonFeature feature = response.features().getFirst();
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

package com.blablatwo.city;

import com.blablatwo.city.geocoding.GeocodedPlace;
import com.blablatwo.city.geocoding.GeocodingClient;
import com.blablatwo.exceptions.GeocodingException;
import com.blablatwo.exceptions.NoSuchCityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for language-aware city resolution.
 * <p>
 * Two resolution methods:
 * <ul>
 *   <li>{@link #resolveCityByPlaceId} - Primary method for mobile/internal DTOs (placeId required)</li>
 *   <li>{@link #resolveCityByName} - For external ingestion (name-based, resolves to placeId)</li>
 * </ul>
 */
@Service
public class CityResolutionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CityResolutionService.class);

    private final CityRepository cityRepository;
    private final GeocodingClient geocodingClient;
    private final CityNameNormalizer cityNameNormalizer;

    public CityResolutionService(CityRepository cityRepository, GeocodingClient geocodingClient, CityNameNormalizer cityNameNormalizer) {
        this.cityRepository = cityRepository;
        this.geocodingClient = geocodingClient;
        this.cityNameNormalizer = cityNameNormalizer;
    }

    /**
     * Resolve a city by placeId. If not in DB, fetches from geocoding and creates.
     * Used by ride creation/update.
     *
     * @param placeId The placeId from the geocoding provider
     * @param lang    Language hint for geocoding lookup ("pl" or "en")
     * @return The resolved City entity
     * @throws NoSuchCityException if city cannot be found in DB or geocoding
     */
    @Transactional
    public City resolveCityByPlaceId(Long placeId, String lang) {
        if (placeId == null) {
            throw new IllegalArgumentException("placeId is required");
        }

        // 1. Try DB first
        Optional<City> existing = cityRepository.findByPlaceId(placeId);
        if (existing.isPresent()) {
            LOGGER.debug("Found existing city by placeId: {}", placeId);
            return existing.get();
        }

        // 2. Fetch from geocoding service
        LOGGER.info("City not in DB, fetching from geocoding: placeId={}, lang={}", placeId, lang);
        Optional<GeocodedPlace> place = geocodingClient.lookupByPlaceId(placeId, lang);
        if (place.isEmpty()) {
            throw new NoSuchCityException(placeId);
        }

        // 3. Create and save city
        return createCityFromGeocodedPlace(place.get());
    }

    /**
     * Resolve a city by name using geocoding API.
     * Used by external ingestion (scraper microservice) and admin/migration tools.
     *
     * @param name City name to resolve
     * @param lang Language hint ("pl" or "en"); if null, tries both languages
     * @return The resolved City entity
     * @throws NoSuchCityException if the city cannot be resolved
     * @throws GeocodingException if the geocoding service is unavailable
     */
    @Transactional
    public City resolveCityByName(String name, String lang) {
        if (name == null || name.isBlank()) {
            throw new NoSuchCityException("City name is required");
        }

        String normalizedName = cityNameNormalizer.normalize(name);

        // 1. Try database lookup first (by normalized name)
        Optional<City> existingByNormPl = cityRepository.findByNormNamePl(normalizedName);
        if (existingByNormPl.isPresent()) {
            LOGGER.debug("Found city by normNamePl: {}", normalizedName);
            return existingByNormPl.get();
        }

        Optional<City> existingByNormEn = cityRepository.findByNormNameEn(normalizedName);
        if (existingByNormEn.isPresent()) {
            LOGGER.debug("Found city by normNameEn: {}", normalizedName);
            return existingByNormEn.get();
        }

        // 2. Call geocoding API with lang strategy
        GeocodedPlace resolvedPlace = resolveViaGeocoding(name, lang);

        // 3. Check if city with this placeId already exists
        Optional<City> existingByPlaceId = cityRepository.findByPlaceId(resolvedPlace.placeId());
        if (existingByPlaceId.isPresent()) {
            City existing = existingByPlaceId.get();
            LOGGER.debug("Found existing city by placeId: {}", resolvedPlace.placeId());
            // Optionally enrich with the new name if missing
            enrichCityWithName(existing, resolvedPlace);
            return existing;
        }

        // 4. Create new city
        return createCityFromGeocodedPlace(resolvedPlace);
    }

    private GeocodedPlace resolveViaGeocoding(String name, String lang) {
        if (lang != null && !lang.isBlank()) {
            // Use specified language
            return geocodingClient.bestMatchCity(name, lang)
                    .orElseThrow(() -> new NoSuchCityException(name));
        }

        // Try Polish first, then English
        Optional<GeocodedPlace> plResult = geocodingClient.bestMatchCity(name, "pl");
        if (plResult.isPresent()) {
            return plResult.get();
        }

        Optional<GeocodedPlace> enResult = geocodingClient.bestMatchCity(name, "en");
        if (enResult.isPresent()) {
            return enResult.get();
        }

        throw new NoSuchCityException(name);
    }

    private void enrichCityWithName(City city, GeocodedPlace place) {
        boolean changed = false;

        if ("pl".equalsIgnoreCase(place.lang()) && city.getNamePl() == null) {
            city.setNamePl(place.name());
            city.setNormNamePl(cityNameNormalizer.normalize(place.name()));
            changed = true;
        } else if ("en".equalsIgnoreCase(place.lang()) && city.getNameEn() == null) {
            city.setNameEn(place.name());
            city.setNormNameEn(cityNameNormalizer.normalize(place.name()));
            changed = true;
        }

        if (city.getCountryCode() == null && place.countryCode() != null) {
            city.setCountryCode(place.countryCode());
            changed = true;
        }

        if (city.getPopulation() == null && place.population() != null) {
            city.setPopulation(place.population());
            changed = true;
        }

        if (changed) {
            cityRepository.save(city);
            LOGGER.info("Enriched city {} with additional data", city.getPlaceId());
        }
    }

    private City createCityFromGeocodedPlace(GeocodedPlace place) {
        LOGGER.info("Creating new city from geocoding: placeId={}, name={}, lang={}",
                place.placeId(), place.name(), place.lang());

        String normalizedName = cityNameNormalizer.normalize(place.name());
        City.CityBuilder builder = City.builder()
                .placeId(place.placeId())
                .countryCode(place.countryCode())
                .population(place.population());

        if ("en".equalsIgnoreCase(place.lang())) {
            builder.nameEn(place.name())
                    .normNameEn(normalizedName)
                    .namePl(place.name())      // Fallback to provided name
                    .normNamePl(normalizedName);
        } else {
            builder.namePl(place.name())
                    .normNamePl(normalizedName)
                    .nameEn(place.name())      // Fallback to provided name
                    .normNameEn(normalizedName);
        }

        return cityRepository.save(builder.build());
    }
}

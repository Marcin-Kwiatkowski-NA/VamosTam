package com.blablatwo.city;

import org.springframework.stereotype.Component;

import java.text.Normalizer;

/**
 * Utility for normalizing city names for comparison and deduplication.
 */
@Component
public class CityNameNormalizer {

    /**
     * Normalize a city name by removing diacritics and converting to lowercase.
     *
     * @param name City name to normalize
     * @return Normalized name, or null if input is null
     */
    public String normalize(String name) {
        if (name == null) {
            return null;
        }
        String normalized = Normalizer.normalize(name.toLowerCase().trim(), Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}

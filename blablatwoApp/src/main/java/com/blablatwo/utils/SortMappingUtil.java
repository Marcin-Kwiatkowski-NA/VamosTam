package com.blablatwo.utils;

import org.springframework.data.domain.Sort;

import java.util.Set;

public final class SortMappingUtil {

    private SortMappingUtil() {
    }

    /**
     * Translates an API-level sort field name to internal JPA sort.
     * <p>
     * "departureTime" maps to the composite embedded path: timeSlot.departureDate + timeSlot.departureTime.
     *
     * @throws IllegalArgumentException if sortBy is not in the allowed set
     */
    public static Sort translateSort(String sortBy, String sortDir, Set<String> allowedFields) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        if ("departureTime".equals(sortBy)) {
            return Sort.by(direction, "timeSlot.departureDate")
                    .and(Sort.by(direction, "timeSlot.departureTime"));
        }

        if (allowedFields.contains(sortBy)) {
            return Sort.by(direction, sortBy);
        }

        throw new IllegalArgumentException("Unknown sort field: " + sortBy);
    }
}

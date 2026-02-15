package com.blablatwo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SpatialIndexInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpatialIndexInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public SpatialIndexInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createSpatialIndexes() {
        try {
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_location_coordinates_gist ON location USING GIST (coordinates)");
            LOGGER.info("Spatial GIST index on location.coordinates ensured");
        } catch (Exception e) {
            LOGGER.warn("Could not create spatial index (may not be supported by current DB): {}", e.getMessage());
        }
    }
}

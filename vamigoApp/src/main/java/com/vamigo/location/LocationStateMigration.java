package com.vamigo.location;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class LocationStateMigration implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocationStateMigration.class);

    private final LocationRepository locationRepository;
    private final LocationResolutionService locationResolutionService;

    public LocationStateMigration(LocationRepository locationRepository,
                                  LocationResolutionService locationResolutionService) {
        this.locationRepository = locationRepository;
        this.locationResolutionService = locationResolutionService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Location> locations = locationRepository.findByStatePlIsNull();
        if (locations.isEmpty()) {
            LOGGER.debug("No locations need state migration");
            return;
        }

        LOGGER.info("Migrating bilingual state for {} locations", locations.size());
        int success = 0;

        for (Location location : locations) {
            try {
                var plFields = locationResolutionService.reverseByOsmId(
                        location.getLatitude(), location.getLongitude(), location.getOsmId(), LocationLang.pl);
                var enFields = locationResolutionService.reverseByOsmId(
                        location.getLatitude(), location.getLongitude(), location.getOsmId(), LocationLang.en);

                location.setStatePl(plFields.map(LocationResolutionService.ReversedFields::state).orElse(null));
                location.setStateEn(enFields.map(LocationResolutionService.ReversedFields::state).orElse(null));
                success++;
            } catch (Exception e) {
                LOGGER.warn("Failed to migrate state for location osmId={}: {}", location.getOsmId(), e.getMessage());
            }
        }

        LOGGER.info("State migration complete: {}/{} locations updated", success, locations.size());
    }
}

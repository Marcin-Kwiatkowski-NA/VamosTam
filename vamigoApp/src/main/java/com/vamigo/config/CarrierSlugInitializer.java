package com.vamigo.config;

import com.vamigo.user.CarrierProfile;
import com.vamigo.user.CarrierProfileRepository;
import com.vamigo.user.SlugUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class CarrierSlugInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CarrierSlugInitializer.class);

    private final CarrierProfileRepository carrierProfileRepository;

    public CarrierSlugInitializer(CarrierProfileRepository carrierProfileRepository) {
        this.carrierProfileRepository = carrierProfileRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeMissingSlugs() {
        List<CarrierProfile> carriersWithoutSlug = carrierProfileRepository.findAll()
                .stream()
                .filter(c -> c.getSlug() == null || c.getSlug().isBlank())
                .toList();

        if (carriersWithoutSlug.isEmpty()) {
            return;
        }

        LOGGER.info("Initializing slugs for {} carriers", carriersWithoutSlug.size());

        for (CarrierProfile carrier : carriersWithoutSlug) {
            String baseSlug = SlugUtils.generateSlug(carrier.getCompanyName());
            if (baseSlug.length() < 3) {
                baseSlug = "carrier-" + carrier.getId();
            }
            String uniqueSlug = SlugUtils.makeUnique(baseSlug, carrierProfileRepository::existsBySlug);
            carrier.setSlug(uniqueSlug);
            carrierProfileRepository.save(carrier);
            LOGGER.info("Assigned slug '{}' to carrier {}", uniqueSlug, carrier.getId());
        }
    }
}

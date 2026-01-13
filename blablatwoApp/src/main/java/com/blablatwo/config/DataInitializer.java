package com.blablatwo.config;

import com.blablatwo.traveler.FacebookProxyConstants;
import com.blablatwo.traveler.Role;
import com.blablatwo.traveler.TravelerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataInitializer.class);

    private final TravelerRepository travelerRepository;

    public DataInitializer(TravelerRepository travelerRepository) {
        this.travelerRepository = travelerRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        createFacebookProxyUserIfNotExists();
    }

    private void createFacebookProxyUserIfNotExists() {
        if (travelerRepository.existsById(FacebookProxyConstants.FACEBOOK_PROXY_ID)) {
            LOGGER.debug("Facebook proxy user already exists with ID: {}",
                    FacebookProxyConstants.FACEBOOK_PROXY_ID);
            return;
        }

        travelerRepository.insertFacebookProxyIfNotExists(
                FacebookProxyConstants.FACEBOOK_PROXY_ID,
                FacebookProxyConstants.FACEBOOK_PROXY_USERNAME,
                FacebookProxyConstants.FACEBOOK_PROXY_EMAIL,
                FacebookProxyConstants.FACEBOOK_PROXY_NAME,
                Role.DRIVER.name(),
                1
        );

        LOGGER.info("Created Facebook proxy user with ID: {}",
                FacebookProxyConstants.FACEBOOK_PROXY_ID);
    }
}

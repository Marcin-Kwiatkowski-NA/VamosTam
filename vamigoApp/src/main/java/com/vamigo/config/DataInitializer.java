package com.vamigo.config;

import com.vamigo.user.AccountStatus;
import com.vamigo.user.Role;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import com.vamigo.user.UserProfile;
import com.vamigo.user.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataInitializer.class);

    public static final String FACEBOOK_BOT_EMAIL = "facebook-bot@vamos.internal";
    private static final String FACEBOOK_BOT_NAME = "Facebook Rides";

    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(UserAccountRepository userAccountRepository,
                           UserProfileRepository userProfileRepository,
                           JdbcTemplate jdbcTemplate) {
        this.userAccountRepository = userAccountRepository;
        this.userProfileRepository = userProfileRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        createFacebookBotIfNotExists();
        migrateTimePrecision();
    }

    private void createFacebookBotIfNotExists() {
        if (userAccountRepository.existsByEmail(FACEBOOK_BOT_EMAIL)) {
            LOGGER.debug("Facebook bot user already exists with email: {}", FACEBOOK_BOT_EMAIL);
            return;
        }

        UserAccount bot = UserAccount.builder()
                .email(FACEBOOK_BOT_EMAIL)
                .status(AccountStatus.ACTIVE)
                .build();
        bot.addRole(Role.SYSTEM);

        UserAccount savedBot = userAccountRepository.save(bot);

        UserProfile profile = UserProfile.builder()
                .account(savedBot)
                .displayName(FACEBOOK_BOT_NAME)
                .build();

        userProfileRepository.save(profile);

        LOGGER.info("Created Facebook bot user with email: {}", FACEBOOK_BOT_EMAIL);
    }

    private void migrateTimePrecision() {
        try {
            int ridesMigrated = jdbcTemplate.update(
                    "UPDATE ride SET time_precision = CASE WHEN is_time_approximate = true THEN 'APPROXIMATE' ELSE 'EXACT' END WHERE time_precision IS NULL");
            int seatsMigrated = jdbcTemplate.update(
                    "UPDATE seat SET time_precision = CASE WHEN is_time_approximate = true THEN 'APPROXIMATE' ELSE 'EXACT' END WHERE time_precision IS NULL");
            if (ridesMigrated > 0 || seatsMigrated > 0) {
                LOGGER.info("Migrated time_precision: {} rides, {} seats", ridesMigrated, seatsMigrated);
            }
        } catch (Exception e) {
            LOGGER.debug("time_precision migration skipped: {}", e.getMessage());
        }
    }
}

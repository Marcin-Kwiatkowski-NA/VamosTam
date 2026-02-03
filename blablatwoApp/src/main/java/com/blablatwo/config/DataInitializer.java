package com.blablatwo.config;

import com.blablatwo.user.AccountStatus;
import com.blablatwo.user.Role;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import com.blablatwo.user.UserProfile;
import com.blablatwo.user.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataInitializer.class);

    public static final String FACEBOOK_BOT_EMAIL = "facebook-bot@vamos.internal";
    private static final String FACEBOOK_BOT_NAME = "Facebook Rides";

    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;

    public DataInitializer(UserAccountRepository userAccountRepository,
                           UserProfileRepository userProfileRepository) {
        this.userAccountRepository = userAccountRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        createFacebookBotIfNotExists();
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
}

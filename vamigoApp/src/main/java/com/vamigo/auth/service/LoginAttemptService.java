package com.vamigo.auth.service;

import com.vamigo.auth.AuthSecurityProperties;
import com.vamigo.user.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class LoginAttemptService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginAttemptService.class);

    private final UserAccountRepository userAccountRepository;
    private final AuthSecurityProperties properties;
    private final Clock clock;

    public LoginAttemptService(UserAccountRepository userAccountRepository,
                               AuthSecurityProperties properties,
                               Clock clock) {
        this.userAccountRepository = userAccountRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(String email) {
        try {
            userAccountRepository.findByEmail(email.toLowerCase()).ifPresent(account -> {
                account.recordFailedLogin(
                        Instant.now(clock),
                        properties.accountLock().maxFailedAttempts(),
                        properties.accountLock().lockDurationMinutes()
                );
                userAccountRepository.save(account);
            });
        } catch (ObjectOptimisticLockingFailureException e) {
            LOGGER.debug("Concurrent login attempt update for {}, skipping", email);
        }
    }

    @Transactional
    public void resetAttempts(String email) {
        userAccountRepository.findByEmail(email.toLowerCase()).ifPresent(account -> {
            if (account.getFailedLoginAttempts() > 0) {
                account.resetFailedLoginAttempts();
                userAccountRepository.save(account);
            }
        });
    }
}

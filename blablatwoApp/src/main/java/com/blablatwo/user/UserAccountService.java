package com.blablatwo.user;

import com.blablatwo.auth.AuthProvider;
import com.blablatwo.user.exception.DuplicateEmailException;
import com.blablatwo.user.exception.NoSuchUserException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Optional<UserAccount> findById(Long id) {
        return userAccountRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> findByEmail(String email) {
        return userAccountRepository.findByEmail(email.toLowerCase());
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> findByGoogleId(String googleId) {
        return userAccountRepository.findByGoogleId(googleId);
    }

    @Transactional
    public UserAccount createWithEmailPassword(String email, String password, String displayName) {
        String normalizedEmail = email.toLowerCase();

        if (userAccountRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        Set<AuthProvider> providers = new HashSet<>();
        providers.add(AuthProvider.EMAIL);

        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);

        UserAccount account = UserAccount.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(password))
                .providers(providers)
                .status(AccountStatus.ACTIVE)
                .roles(roles)
                .build();

        UserAccount savedAccount = userAccountRepository.save(account);

        UserProfile profile = UserProfile.builder()
                .account(savedAccount)
                .displayName(displayName)
                .stats(new UserStats())
                .build();

        userProfileRepository.save(profile);

        return savedAccount;
    }

    @Transactional
    public UserAccount createOrUpdateGoogleUser(String email, String googleId, String name, String pictureUrl) {
        String normalizedEmail = email.toLowerCase();

        // Check if user exists by googleId
        Optional<UserAccount> existingByGoogleId = userAccountRepository.findByGoogleId(googleId);
        if (existingByGoogleId.isPresent()) {
            UserAccount account = existingByGoogleId.get();
            account.setEmailVerifiedAt(Instant.now());

            // Update profile picture if changed
            userProfileRepository.findById(account.getId()).ifPresent(profile -> {
                if (pictureUrl != null && !pictureUrl.equals(profile.getAvatarUrl())) {
                    profile.setAvatarUrl(pictureUrl);
                }
            });

            return userAccountRepository.save(account);
        }

        // Check if user exists by email
        Optional<UserAccount> existingByEmail = userAccountRepository.findByEmail(normalizedEmail);
        if (existingByEmail.isPresent()) {
            UserAccount account = existingByEmail.get();
            // Link Google account
            account.addProvider(AuthProvider.GOOGLE);
            account.setGoogleId(googleId);
            account.setEmailVerifiedAt(Instant.now());

            // Update profile picture if not set
            userProfileRepository.findById(account.getId()).ifPresent(profile -> {
                if (profile.getAvatarUrl() == null && pictureUrl != null) {
                    profile.setAvatarUrl(pictureUrl);
                }
            });

            return userAccountRepository.save(account);
        }

        // Create new user
        Set<AuthProvider> providers = new HashSet<>();
        providers.add(AuthProvider.GOOGLE);

        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);

        UserAccount account = UserAccount.builder()
                .email(normalizedEmail)
                .providers(providers)
                .status(AccountStatus.ACTIVE)
                .roles(roles)
                .googleId(googleId)
                .emailVerifiedAt(Instant.now())
                .build();

        UserAccount savedAccount = userAccountRepository.save(account);

        UserProfile profile = UserProfile.builder()
                .account(savedAccount)
                .displayName(name)
                .avatarUrl(pictureUrl)
                .stats(new UserStats())
                .build();

        userProfileRepository.save(profile);

        return savedAccount;
    }

    @Transactional(readOnly = true)
    public UserAccount getById(Long id) {
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new NoSuchUserException(id));
    }
}

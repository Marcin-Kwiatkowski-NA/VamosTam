package com.blablatwo.user;

import com.blablatwo.auth.AuthProvider;
import com.blablatwo.auth.verification.EmailVerificationTokenRepository;
import com.blablatwo.messaging.ConversationRepository;
import com.blablatwo.messaging.MessageRepository;
import com.blablatwo.ride.RideBookingRepository;
import com.blablatwo.ride.RideExternalMetaRepository;
import com.blablatwo.ride.RideRepository;
import com.blablatwo.seat.SeatExternalMetaRepository;
import com.blablatwo.seat.SeatRepository;
import com.blablatwo.user.exception.DuplicateEmailException;
import com.blablatwo.user.exception.NoSuchUserException;
import com.blablatwo.vehicle.VehicleRepository;
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
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final RideBookingRepository rideBookingRepository;
    private final RideExternalMetaRepository rideExternalMetaRepository;
    private final SeatExternalMetaRepository seatExternalMetaRepository;
    private final RideRepository rideRepository;
    private final SeatRepository seatRepository;
    private final VehicleRepository vehicleRepository;

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
    public UserAccount createOrUpdateGoogleUser(String email, String googleId, String name) {
        String normalizedEmail = email.toLowerCase();

        // Check if user exists by googleId
        Optional<UserAccount> existingByGoogleId = userAccountRepository.findByGoogleId(googleId);
        if (existingByGoogleId.isPresent()) {
            UserAccount account = existingByGoogleId.get();
            account.setEmailVerifiedAt(Instant.now());

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
                .stats(new UserStats())
                .build();

        userProfileRepository.save(profile);

        return savedAccount;
    }

    @Transactional
    public void deleteAccount(Long userId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));

        // 1. Email verification tokens
        emailVerificationTokenRepository.deleteByUserId(userId);

        // 2. Messages in user's conversations, then conversations
        var conversations = conversationRepository.findAllByParticipantId(userId);
        if (!conversations.isEmpty()) {
            var conversationIds = conversations.stream().map(c -> c.getId()).toList();
            messageRepository.deleteByConversationIdIn(conversationIds);
            conversationRepository.deleteAllByParticipantId(userId);
        }

        // 3. Bookings where user is passenger on OTHER people's rides
        rideBookingRepository.deleteByPassengerId(userId);

        // 4. External meta for user's rides (shared PK = ride ID)
        var userRides = rideRepository.findByDriverIdOrderByDepartureTimeAsc(userId);
        if (!userRides.isEmpty()) {
            var rideIds = userRides.stream().map(r -> r.getId()).toList();
            rideExternalMetaRepository.deleteAllByIdIn(rideIds);
        }

        // 5. External meta for user's seats (shared PK = seat ID)
        var userSeats = seatRepository.findByPassengerIdOrderByDepartureTimeAsc(userId);
        if (!userSeats.isEmpty()) {
            var seatIds = userSeats.stream().map(s -> s.getId()).toList();
            seatExternalMetaRepository.deleteAllByIdIn(seatIds);
        }

        // 6. Rides (cascades to ride_stop, ride_booking via CascadeType.ALL)
        rideRepository.deleteAll(userRides);

        // 7. Seats
        seatRepository.deleteAll(userSeats);

        // 8. Vehicles
        vehicleRepository.deleteByOwnerId(userId);

        // 9. User profile
        userProfileRepository.deleteById(userId);

        // 10. User account (cascades element collections: providers, roles)
        userAccountRepository.delete(account);
    }

    @Transactional(readOnly = true)
    public UserAccount getById(Long id) {
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new NoSuchUserException(id));
    }
}

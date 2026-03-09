package com.vamigo.auth.service;

import com.vamigo.auth.dto.AuthResponse;
import com.vamigo.auth.dto.LoginRequest;
import com.vamigo.auth.dto.RefreshTokenRequest;
import com.vamigo.auth.dto.RegisterRequest;
import com.vamigo.auth.verification.PasswordResetService;
import com.vamigo.auth.event.OnRegistrationCompleteEvent;
import com.vamigo.auth.exception.EmailAlreadyVerifiedException;
import com.vamigo.auth.exception.InvalidTokenException;
import com.vamigo.auth.verification.EmailVerificationService;
import com.vamigo.user.AvatarService;
import com.vamigo.user.SecurityUser;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import com.vamigo.user.UserAccountService;
import com.vamigo.user.UserProfile;
import com.vamigo.user.UserProfileMapper;
import com.vamigo.user.UserProfileRepository;
import com.vamigo.user.dto.UserProfileDto;
import com.vamigo.user.exception.NoSuchUserException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.springframework.context.i18n.LocaleContextHolder;

@Service
@Transactional
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserAccountService userAccountService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserProfileMapper userProfileMapper;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final ApplicationEventPublisher eventPublisher;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final PasswordEncoder passwordEncoder;
    private final AvatarService avatarService;

    public AuthService(UserAccountRepository userAccountRepository,
                       UserProfileRepository userProfileRepository,
                       UserAccountService userAccountService,
                       JwtTokenProvider jwtTokenProvider,
                       UserProfileMapper userProfileMapper,
                       GoogleTokenVerifier googleTokenVerifier,
                       ApplicationEventPublisher eventPublisher,
                       EmailVerificationService emailVerificationService,
                       PasswordResetService passwordResetService,
                       PasswordEncoder passwordEncoder,
                       AvatarService avatarService) {
        this.userAccountRepository = userAccountRepository;
        this.userProfileRepository = userProfileRepository;
        this.userAccountService = userAccountService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userProfileMapper = userProfileMapper;
        this.googleTokenVerifier = googleTokenVerifier;
        this.eventPublisher = eventPublisher;
        this.emailVerificationService = emailVerificationService;
        this.passwordResetService = passwordResetService;
        this.passwordEncoder = passwordEncoder;
        this.avatarService = avatarService;
    }

    public AuthResponse login(LoginRequest request, AuthenticationManager authenticationManager) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().toLowerCase(),
                        request.password()
                )
        );

        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        UserAccount account = securityUser.getAccount();

        return buildAuthResponse(account);
    }

    public AuthResponse register(RegisterRequest request) {
        String displayName = request.displayName();
        if (displayName == null || displayName.isBlank()) {
            displayName = request.email().split("@")[0];
        }

        UserAccount account = userAccountService.createWithEmailPassword(
                request.email(),
                request.password(),
                displayName
        );

        eventPublisher.publishEvent(new OnRegistrationCompleteEvent(account, LocaleContextHolder.getLocale()));

        return buildAuthResponse(account);
    }

    public void verifyEmail(String token) {
        emailVerificationService.verifyEmail(token);
    }

    public void resendVerificationEmail(Long userId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));
        if (account.getEmailVerifiedAt() != null) {
            throw new EmailAlreadyVerifiedException();
        }
        emailVerificationService.sendVerificationEmail(account, LocaleContextHolder.getLocale());
    }

    public AuthResponse authenticateWithGoogle(String idToken) throws GeneralSecurityException, IOException {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(idToken);

        String email = payload.getEmail();
        String googleId = payload.getSubject();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");

        UserAccount account = userAccountService.createOrUpdateGoogleUser(email, googleId, name);

        if (pictureUrl != null) {
            UserProfile profile = userProfileRepository.findById(account.getId()).orElse(null);
            if (profile != null && profile.getAvatarObjectKey() == null) {
                avatarService.importAvatarFromUrl(account.getId(), pictureUrl);
            }
        }

        return buildAuthResponse(account);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException("Token is not a refresh token");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException("User not found"));

        int tokenVersion = jwtTokenProvider.getTokenVersionFromToken(refreshToken);
        if (tokenVersion != account.getTokenVersion()) {
            throw new InvalidTokenException("Token has been revoked");
        }

        return buildAuthResponse(account);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getCurrentUserById(Long userId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));
        return userProfileMapper.toDto(account, profile);
    }

    public void forgotPassword(String email, java.util.Locale locale) {
        passwordResetService.sendResetEmail(email, locale);
    }

    public void resetPassword(String token, String newPassword) {
        passwordResetService.resetPassword(token, newPassword);
    }

    public AuthResponse changePassword(Long userId, String currentPassword, String newPassword) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));

        if (account.getPasswordHash() == null) {
            throw new BadCredentialsException("Account does not have a password");
        }

        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        account.setPasswordHash(passwordEncoder.encode(newPassword));
        account.incrementTokenVersion();
        userAccountRepository.save(account);

        return buildAuthResponse(account);
    }

    private AuthResponse buildAuthResponse(UserAccount account) {
        String accessToken = jwtTokenProvider.generateToken(account);
        String refreshToken = jwtTokenProvider.generateRefreshToken(account);

        UserProfile profile = userProfileRepository.findById(account.getId())
                .orElseThrow(() -> new NoSuchUserException(account.getId()));

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtTokenProvider.getExpirationMs(),
                jwtTokenProvider.getRefreshExpirationMs(),
                userProfileMapper.toDto(account, profile)
        );
    }
}

package com.blablatwo.auth.service;

import com.blablatwo.auth.dto.AuthResponse;
import com.blablatwo.auth.dto.LoginRequest;
import com.blablatwo.auth.dto.RefreshTokenRequest;
import com.blablatwo.auth.dto.RegisterRequest;
import com.blablatwo.auth.exception.InvalidTokenException;
import com.blablatwo.user.SecurityUser;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import com.blablatwo.user.UserAccountService;
import com.blablatwo.user.UserProfile;
import com.blablatwo.user.UserProfileMapper;
import com.blablatwo.user.UserProfileRepository;
import com.blablatwo.user.dto.UserProfileDto;
import com.blablatwo.user.exception.NoSuchUserException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Service
@Transactional
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserAccountService userAccountService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserProfileMapper userProfileMapper;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthService(UserAccountRepository userAccountRepository,
                       UserProfileRepository userProfileRepository,
                       UserAccountService userAccountService,
                       JwtTokenProvider jwtTokenProvider,
                       UserProfileMapper userProfileMapper,
                       GoogleTokenVerifier googleTokenVerifier) {
        this.userAccountRepository = userAccountRepository;
        this.userProfileRepository = userProfileRepository;
        this.userAccountService = userAccountService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userProfileMapper = userProfileMapper;
        this.googleTokenVerifier = googleTokenVerifier;
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

        return buildAuthResponse(account);
    }

    public AuthResponse authenticateWithGoogle(String idToken) throws GeneralSecurityException, IOException {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(idToken);

        String email = payload.getEmail();
        String googleId = payload.getSubject();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");

        UserAccount account = userAccountService.createOrUpdateGoogleUser(email, googleId, name, pictureUrl);
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

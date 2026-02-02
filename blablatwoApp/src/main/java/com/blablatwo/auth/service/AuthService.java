package com.blablatwo.auth.service;

import com.blablatwo.auth.AuthProvider;
import com.blablatwo.auth.dto.AuthResponse;
import com.blablatwo.auth.dto.LoginRequest;
import com.blablatwo.auth.dto.RefreshTokenRequest;
import com.blablatwo.auth.dto.RegisterRequest;
import com.blablatwo.auth.exception.InvalidTokenException;
import com.blablatwo.exceptions.NoSuchTravelerException;
import com.blablatwo.traveler.*;
import com.blablatwo.traveler.user.GoogleUser;
import com.blablatwo.traveler.user.SecurityUser;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final TravelerRepository travelerRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TravelerMapper travelerMapper;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthService(TravelerRepository travelerRepository,
                       JwtTokenProvider jwtTokenProvider,
                       TravelerMapper travelerMapper,
                       PasswordEncoder passwordEncoder,
                       GoogleTokenVerifier googleTokenVerifier) {
        this.travelerRepository = travelerRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.travelerMapper = travelerMapper;
        this.passwordEncoder = passwordEncoder;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    public AuthResponse login(LoginRequest request, AuthenticationManager authenticationManager) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        Traveler traveler = travelerRepository.findByUsername(securityUser.getUsername())
                .orElseThrow(() -> new NoSuchTravelerException("User not found"));

        return buildAuthResponse(traveler);
    }

    public AuthResponse register(RegisterRequest request) {
        if (travelerRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateEmailException("Email already exists");
        }

        if (travelerRepository.findByUsername(request.email()).isPresent()) {
            throw new DuplicateEmailException("Username already exists");
        }

        Traveler newTraveler = Traveler.builder()
                .email(request.email())
                .username(request.email())
                .password(passwordEncoder.encode(request.password()))
                .enabled(1)
                .role(Role.PASSENGER)
                .build();

        Traveler savedTraveler = travelerRepository.save(newTraveler);
        return buildAuthResponse(savedTraveler);
    }

    public AuthResponse authenticateWithGoogle(String idToken) throws GeneralSecurityException, IOException {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(idToken);

        String email = payload.getEmail();
        String googleId = payload.getSubject();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");

        Traveler traveler = processGoogleUser(email, googleId, name, pictureUrl);
        return buildAuthResponse(traveler);
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
        Traveler traveler = travelerRepository.findById(userId)
                .orElseThrow(() -> new NoSuchTravelerException("User not found"));

        return buildAuthResponse(traveler);
    }

    @Transactional(readOnly = true)
    public TravelerResponseDto getCurrentUserById(Long travelerId) {
        Traveler traveler = travelerRepository.findById(travelerId)
                .orElseThrow(() -> new NoSuchTravelerException("User not found"));
        return travelerMapper.travelerEntityToTravelerResponseDto(traveler);
    }

    private Traveler processGoogleUser(String email, String googleId, String name, String pictureUrl) {
        return travelerRepository.findByGoogleUser_GoogleId(googleId)
                .map(traveler -> updateExistingGoogleUser(traveler, pictureUrl))
                .orElseGet(() ->
                        travelerRepository.findByEmail(email)
                                .map(traveler -> linkGoogleAccount(traveler, googleId, pictureUrl))
                                .orElseGet(() ->
                                        createNewGoogleUser(email, googleId, name, pictureUrl)
                                )
                );
    }

    private Traveler updateExistingGoogleUser(Traveler traveler, String pictureUrl) {
        if (traveler.getGoogleUser() != null) {
            traveler.getGoogleUser().setPictureUrl(pictureUrl);
        }
        return travelerRepository.save(traveler);
    }

    private Traveler linkGoogleAccount(Traveler traveler, String googleId, String pictureUrl) {
        GoogleUser googleUser = traveler.getGoogleUser();
        if (googleUser == null) {
            googleUser = new GoogleUser();
            traveler.setGoogleUser(googleUser);
        }
        googleUser.setGoogleId(googleId);
        googleUser.setPictureUrl(pictureUrl);
        googleUser.setEmailVerified(true);
        googleUser.setAuthProvider(AuthProvider.GOOGLE);
        return travelerRepository.save(traveler);
    }

    private Traveler createNewGoogleUser(String email, String googleId, String name, String pictureUrl) {
        String username = generateUsernameFromEmail(email);

        GoogleUser googleUser = GoogleUser.builder()
                .googleId(googleId)
                .pictureUrl(pictureUrl)
                .authProvider(AuthProvider.GOOGLE)
                .emailVerified(true)
                .build();

        Traveler newTraveler = Traveler.builder()
                .email(email)
                .username(username)
                .name(name)
                .googleUser(googleUser)
                .enabled(1)
                .role(Role.PASSENGER)
                .build();

        return travelerRepository.save(newTraveler);
    }

    private String generateUsernameFromEmail(String email) {
        String base = email.split("@")[0];
        if (travelerRepository.findByUsername(base).isPresent()) {
            return base + "_" + UUID.randomUUID().toString().substring(0, 6);
        }
        return base;
    }

    private AuthResponse buildAuthResponse(Traveler traveler) {
        String accessToken = jwtTokenProvider.generateToken(traveler);
        String refreshToken = jwtTokenProvider.generateRefreshToken(traveler);

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtTokenProvider.getExpirationMs(),
                jwtTokenProvider.getRefreshExpirationMs(),
                travelerMapper.travelerEntityToTravelerResponseDto(traveler)
        );
    }
}

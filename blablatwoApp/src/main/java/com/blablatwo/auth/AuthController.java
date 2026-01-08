package com.blablatwo.auth;

import com.blablatwo.auth.dto.AuthResponse;
import com.blablatwo.auth.dto.GoogleTokenRequest;
import com.blablatwo.auth.dto.LoginRequest;
import com.blablatwo.auth.dto.RegisterRequest;
import com.blablatwo.auth.service.GoogleTokenVerifier;
import com.blablatwo.auth.service.JwtTokenProvider;
import com.blablatwo.traveler.Role;
import com.blablatwo.traveler.*;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final TravelerRepository travelerRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TravelerMapper travelerMapper;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    public AuthController(GoogleTokenVerifier googleTokenVerifier,
                          TravelerRepository travelerRepository,
                          JwtTokenProvider jwtTokenProvider,
                          TravelerMapper travelerMapper,
                          AuthenticationManager authenticationManager,
                          PasswordEncoder passwordEncoder) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.travelerRepository = travelerRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.travelerMapper = travelerMapper;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> authenticateWithGoogle(
            @Valid @RequestBody GoogleTokenRequest request) throws GeneralSecurityException, IOException {

        // Verify Google ID token
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(request.idToken());

        String email = payload.getEmail();
        String googleId = payload.getSubject();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");

        // Find or create/link user
        Traveler traveler = processGoogleUser(email, googleId, name, pictureUrl);

        // Generate JWT
        String jwt = jwtTokenProvider.generateToken(traveler);

        return ResponseEntity.ok(new AuthResponse(
                jwt,
                jwtTokenProvider.getExpirationMs(),
                travelerMapper.travelerEntityToTravelerResponseDto(traveler)
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        Traveler traveler = travelerRepository.findByUsername(securityUser.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String jwt = jwtTokenProvider.generateToken(traveler);

        return ResponseEntity.ok(new AuthResponse(
                jwt,
                jwtTokenProvider.getExpirationMs(),
                travelerMapper.travelerEntityToTravelerResponseDto(traveler)
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // Check if email already exists
        if (travelerRepository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // Check if username (email) already exists
        if (travelerRepository.findByUsername(request.email()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // Create new user with email as username
        Traveler newTraveler = Traveler.builder()
                .email(request.email())
                .username(request.email())
                .password(passwordEncoder.encode(request.password()))
                .enabled(1)
                .role(Role.PASSENGER)
                .build();

        Traveler savedTraveler = travelerRepository.save(newTraveler);

        // Generate JWT for auto-login
        String jwt = jwtTokenProvider.generateToken(savedTraveler);

        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(
                jwt,
                jwtTokenProvider.getExpirationMs(),
                travelerMapper.travelerEntityToTravelerResponseDto(savedTraveler)
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<TravelerResponseDto> getCurrentUser(
            @AuthenticationPrincipal SecurityUser securityUser) {

        Traveler traveler = travelerRepository.findByUsername(securityUser.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(travelerMapper.travelerEntityToTravelerResponseDto(traveler));
    }

    private Traveler processGoogleUser(String email, String googleId, String name, String pictureUrl) {
        // Priority 1: Find by Google ID (returning user)
        return travelerRepository.findByGoogleUser_GoogleId(googleId)
                .map(traveler -> updateExistingGoogleUser(traveler, pictureUrl))
                .orElseGet(() ->
                        // Priority 2: Find by email (link existing account)
                        travelerRepository.findByEmail(email)
                                .map(traveler -> linkGoogleAccount(traveler, googleId, pictureUrl))
                                .orElseGet(() ->
                                        // Priority 3: Create new OAuth-only user
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
        // Add random suffix if username exists
        if (travelerRepository.findByUsername(base).isPresent()) {
            return base + "_" + UUID.randomUUID().toString().substring(0, 6);
        }
        return base;
    }
}

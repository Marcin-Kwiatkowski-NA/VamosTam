package com.blablatwo.auth;

import com.blablatwo.auth.dto.AuthResponse;
import com.blablatwo.auth.dto.GoogleTokenRequest;
import com.blablatwo.auth.dto.LoginRequest;
import com.blablatwo.auth.service.GoogleTokenVerifier;
import com.blablatwo.auth.service.JwtTokenProvider;
import com.blablatwo.config.Roles;
import com.blablatwo.traveler.*;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final TravelerRepository travelerRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TravelerMapper travelerMapper;
    private final AuthenticationManager authenticationManager;

    public AuthController(GoogleTokenVerifier googleTokenVerifier,
                          TravelerRepository travelerRepository,
                          JwtTokenProvider jwtTokenProvider,
                          TravelerMapper travelerMapper,
                          AuthenticationManager authenticationManager) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.travelerRepository = travelerRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.travelerMapper = travelerMapper;
        this.authenticationManager = authenticationManager;
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

    @GetMapping("/me")
    public ResponseEntity<TravelerResponseDto> getCurrentUser(
            @AuthenticationPrincipal SecurityUser securityUser) {

        Traveler traveler = travelerRepository.findByUsername(securityUser.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(travelerMapper.travelerEntityToTravelerResponseDto(traveler));
    }

    private Traveler processGoogleUser(String email, String googleId, String name, String pictureUrl) {
        // Priority 1: Find by Google ID (returning user)
        return travelerRepository.findByGoogleId(googleId)
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
        traveler.setPictureUrl(pictureUrl);
        return travelerRepository.save(traveler);
    }

    private Traveler linkGoogleAccount(Traveler traveler, String googleId, String pictureUrl) {
        traveler.setGoogleId(googleId);
        traveler.setPictureUrl(pictureUrl);
        traveler.setEmailVerified(true);
        return travelerRepository.save(traveler);
    }

    private Traveler createNewGoogleUser(String email, String googleId, String name, String pictureUrl) {
        String username = generateUsernameFromEmail(email);

        Traveler newTraveler = Traveler.builder()
                .email(email)
                .username(username)
                .googleId(googleId)
                .name(name)
                .pictureUrl(pictureUrl)
                .authProvider(AuthProvider.GOOGLE)
                .emailVerified(true)
                .enabled(1)
                .authority(Roles.ROLE_PASSENGER)
                .type(TravelerType.PASSENGER)
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

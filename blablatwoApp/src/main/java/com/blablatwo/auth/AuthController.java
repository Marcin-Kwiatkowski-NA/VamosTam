package com.blablatwo.auth;

import com.blablatwo.auth.dto.AuthResponse;
import com.blablatwo.auth.dto.GoogleTokenRequest;
import com.blablatwo.auth.dto.LoginRequest;
import com.blablatwo.auth.dto.RefreshTokenRequest;
import com.blablatwo.auth.dto.RegisterRequest;
import com.blablatwo.auth.exception.InvalidTokenException;
import com.blablatwo.auth.service.AuthService;
import com.blablatwo.user.dto.UserProfileDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final String frontendSuccessUrl;
    private final String frontendErrorUrl;

    public AuthController(AuthService authService,
                          AuthenticationManager authenticationManager,
                          @Value("${app.email-verification.frontend-success-url}") String frontendSuccessUrl,
                          @Value("${app.email-verification.frontend-error-url}") String frontendErrorUrl) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.frontendSuccessUrl = frontendSuccessUrl;
        this.frontendErrorUrl = frontendErrorUrl;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request, authenticationManager));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> authenticateWithGoogle(
            @Valid @RequestBody GoogleTokenRequest request) throws GeneralSecurityException, IOException {
        return ResponseEntity.ok(authService.authenticateWithGoogle(request.idToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getCurrentUser(
            @AuthenticationPrincipal AppPrincipal principal) {
        return ResponseEntity.ok(authService.getCurrentUserById(principal.userId()));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        try {
            authService.verifyEmail(token);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendSuccessUrl))
                    .build();
        } catch (InvalidTokenException e) {
            LOGGER.warn("Email verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendErrorUrl))
                    .build();
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(
            @AuthenticationPrincipal AppPrincipal principal) {
        authService.resendVerificationEmail(principal.userId());
        return ResponseEntity.ok().build();
    }
}

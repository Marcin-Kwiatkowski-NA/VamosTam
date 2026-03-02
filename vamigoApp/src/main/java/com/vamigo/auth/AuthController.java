package com.vamigo.auth;

import com.vamigo.auth.dto.AuthResponse;
import com.vamigo.auth.dto.GoogleTokenRequest;
import com.vamigo.auth.dto.LoginRequest;
import com.vamigo.auth.dto.RefreshTokenRequest;
import com.vamigo.auth.dto.RegisterRequest;
import com.vamigo.auth.exception.InvalidTokenException;
import com.vamigo.auth.service.AuthService;
import com.vamigo.user.dto.UserProfileDto;
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
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        boolean success;
        try {
            authService.verifyEmail(token);
            success = true;
        } catch (InvalidTokenException e) {
            LOGGER.warn("Email verification failed: {}", e.getMessage());
            success = false;
        }

        String deepLink = success ? frontendSuccessUrl : frontendErrorUrl;
        String html = buildVerifyResultPage(success, deepLink);

        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
    }

    private String buildVerifyResultPage(boolean success, String deepLink) {
        String icon = success ? "\u2705" : "\u274C";
        String title = success ? "Email Verified!" : "Verification Failed";
        String message = success
                ? "Your email has been verified successfully. You can close this page or open the app."
                : "The verification link is invalid or has expired. Please request a new one in the app.";
        String color = success ? "#2e7d32" : "#c62828";

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                               display: flex; justify-content: center; align-items: center;
                               min-height: 100vh; margin: 0; background: #f5f5f5; }
                        .card { background: white; border-radius: 16px; padding: 48px; text-align: center;
                                max-width: 400px; box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
                        .icon { font-size: 64px; margin-bottom: 16px; }
                        h1 { color: %s; margin: 0 0 12px; font-size: 24px; }
                        p { color: #666; line-height: 1.5; margin: 0 0 24px; }
                        .btn { display: inline-block; background: #00897b; color: white; text-decoration: none;
                               padding: 12px 32px; border-radius: 28px; font-size: 16px; font-weight: 500; }
                    </style>
                    <script>
                        // Try to open the app via deep link
                        setTimeout(function() { window.location.href = "%s"; }, 1500);
                    </script>
                </head>
                <body>
                    <div class="card">
                        <div class="icon">%s</div>
                        <h1>%s</h1>
                        <p>%s</p>
                        <a class="btn" href="%s">Open App</a>
                    </div>
                </body>
                </html>
                """.formatted(title, color, deepLink, icon, title, message, deepLink);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(
            @AuthenticationPrincipal AppPrincipal principal) {
        authService.resendVerificationEmail(principal.userId());
        return ResponseEntity.ok().build();
    }
}

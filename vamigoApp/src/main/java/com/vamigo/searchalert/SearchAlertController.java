package com.vamigo.searchalert;

import com.vamigo.auth.AppPrincipal;
import com.vamigo.searchalert.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SearchAlertController {

    private final SavedSearchService savedSearchService;

    public SearchAlertController(SavedSearchService savedSearchService) {
        this.savedSearchService = savedSearchService;
    }

    // ── Authenticated endpoints ──

    @GetMapping("/me/search-alerts")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<SavedSearchResponseDto>> getAlerts(
            @AuthenticationPrincipal AppPrincipal principal) {
        return ResponseEntity.ok(savedSearchService.getMyAlerts(principal.userId()));
    }

    @PostMapping("/me/search-alerts")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SavedSearchResponseDto> createAlert(
            @AuthenticationPrincipal AppPrincipal principal,
            @Valid @RequestBody CreateSavedSearchRequest request) {
        return ResponseEntity.ok(savedSearchService.createAlert(principal.userId(), request));
    }

    @DeleteMapping("/me/search-alerts/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteAlert(
            @AuthenticationPrincipal AppPrincipal principal,
            @PathVariable Long id) {
        savedSearchService.deleteAlert(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/search-alerts/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SavedSearchResponseDto> toggleAlert(
            @AuthenticationPrincipal AppPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ToggleAlertRequest request) {
        return ResponseEntity.ok(savedSearchService.toggleAlert(principal.userId(), id, request.active()));
    }

    @GetMapping("/me/notification-preferences")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<NotificationPreferenceDto> getPreferences(
            @AuthenticationPrincipal AppPrincipal principal) {
        return ResponseEntity.ok(savedSearchService.getPreferencesDto(principal.userId()));
    }

    @PatchMapping("/me/notification-preferences")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<NotificationPreferenceDto> updatePreferences(
            @AuthenticationPrincipal AppPrincipal principal,
            @RequestBody UpdateNotificationPreferenceRequest request) {
        return ResponseEntity.ok(savedSearchService.updatePreferences(principal.userId(), request));
    }

    // ── Public endpoint ──

    @GetMapping(value = "/public/search-alerts/unsubscribe", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> unsubscribe(@RequestParam String token) {
        savedSearchService.unsubscribeByToken(token);
        // Always return success page regardless of token validity (prevents enumeration)
        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><title>Unsubscribed</title>
                <style>body{font-family:sans-serif;display:flex;justify-content:center;align-items:center;min-height:100vh;margin:0;background:#f5f5f5;}
                .card{background:#fff;border-radius:16px;padding:48px;text-align:center;box-shadow:0 2px 8px rgba(0,0,0,0.1);max-width:400px;}
                h1{color:#0d7377;font-size:24px;margin:0 0 16px;}p{color:#666;font-size:16px;margin:0;}</style>
                </head><body><div class="card">
                <h1>Unsubscribed</h1>
                <p>You've been unsubscribed from search alert emails. You can re-enable them anytime in the app settings.</p>
                </div></body></html>
                """;
        return ResponseEntity.ok(html);
    }
}

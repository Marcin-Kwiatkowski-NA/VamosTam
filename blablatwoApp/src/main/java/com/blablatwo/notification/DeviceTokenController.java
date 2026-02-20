package com.blablatwo.notification;

import com.blablatwo.auth.AppPrincipal;
import com.blablatwo.notification.dto.DeviceTokenRequest;
import com.blablatwo.notification.dto.DeviceTokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    public DeviceTokenController(DeviceTokenService deviceTokenService) {
        this.deviceTokenService = deviceTokenService;
    }

    @PostMapping("/me/device-tokens")
    public ResponseEntity<DeviceTokenResponse> registerToken(
            @Valid @RequestBody DeviceTokenRequest request,
            @AuthenticationPrincipal AppPrincipal principal) {
        DeviceTokenResponse response = deviceTokenService.register(principal.userId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me/device-tokens/{id}")
    public ResponseEntity<Void> unregisterToken(
            @PathVariable Long id,
            @AuthenticationPrincipal AppPrincipal principal) {
        deviceTokenService.unregister(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }
}

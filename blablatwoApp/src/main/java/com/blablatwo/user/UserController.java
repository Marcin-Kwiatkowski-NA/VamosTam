package com.blablatwo.user;

import com.blablatwo.auth.AppPrincipal;
import com.blablatwo.user.dto.UpdateProfileRequest;
import com.blablatwo.user.dto.UserProfileDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;

    @GetMapping
    public ResponseEntity<UserProfileDto> getMyProfile(@AuthenticationPrincipal AppPrincipal principal) {
        UserProfileDto profile = userProfileService.getProfile(principal.userId());
        return ResponseEntity.ok(profile);
    }

    @PatchMapping
    public ResponseEntity<UserProfileDto> updateMyProfile(
            @AuthenticationPrincipal AppPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileDto profile = userProfileService.updateProfile(principal.userId(), request);
        return ResponseEntity.ok(profile);
    }
}

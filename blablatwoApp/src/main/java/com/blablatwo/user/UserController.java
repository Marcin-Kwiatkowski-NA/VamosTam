package com.blablatwo.user;

import com.blablatwo.auth.AppPrincipal;
import com.blablatwo.user.dto.AvatarConfirmRequest;
import com.blablatwo.user.dto.AvatarConfirmResponse;
import com.blablatwo.user.dto.AvatarPresignResponse;
import com.blablatwo.user.dto.UpdateProfileRequest;
import com.blablatwo.user.dto.UserProfileDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;
    private final UserAccountService userAccountService;
    private final AvatarService avatarService;

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

    @DeleteMapping
    public ResponseEntity<Void> deleteMyAccount(@AuthenticationPrincipal AppPrincipal principal) {
        userAccountService.deleteAccount(principal.userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/avatar/presign")
    public ResponseEntity<AvatarPresignResponse> getAvatarPresignUrl(
            @AuthenticationPrincipal AppPrincipal principal,
            @RequestParam String contentType) {
        var response = avatarService.generatePresignedUrl(principal.userId(), contentType);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/avatar/confirm")
    public ResponseEntity<AvatarConfirmResponse> confirmAvatar(
            @AuthenticationPrincipal AppPrincipal principal,
            @Valid @RequestBody AvatarConfirmRequest request) {
        var response = avatarService.confirmAvatar(principal.userId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<Void> removeAvatar(@AuthenticationPrincipal AppPrincipal principal) {
        avatarService.removeAvatar(principal.userId());
        return ResponseEntity.noContent().build();
    }
}

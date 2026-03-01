package com.blablatwo.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AvatarConfirmRequest(
        @NotBlank
        @Pattern(regexp = "^users/\\d+/avatar/.+$")
        String objectKey
) {}

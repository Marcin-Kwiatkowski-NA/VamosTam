package com.blablatwo.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(max = 100, message = "Display name cannot exceed 100 characters")
    String displayName,

    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    String bio,

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    String phoneNumber
) {}

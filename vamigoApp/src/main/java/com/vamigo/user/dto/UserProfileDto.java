package com.vamigo.user.dto;

import com.vamigo.user.AccountStatus;

public record UserProfileDto(
    Long id,
    String email,
    AccountStatus status,
    String displayName,
    String phoneNumber,
    String avatarUrl,
    String bio,
    UserStatsDto stats
) {}

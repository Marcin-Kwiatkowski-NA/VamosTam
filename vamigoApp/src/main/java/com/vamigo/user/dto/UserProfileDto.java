package com.vamigo.user.dto;

import com.vamigo.user.AccountStatus;
import com.vamigo.user.AccountType;

public record UserProfileDto(
    Long id,
    String email,
    AccountStatus status,
    String displayName,
    String phoneNumber,
    String avatarUrl,
    String bio,
    boolean isEmailVerified,
    boolean isPhoneVerified,
    AccountType accountType,
    UserStatsDto stats
) {}

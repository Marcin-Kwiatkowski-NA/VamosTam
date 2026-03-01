package com.blablatwo.user.dto;

public record AvatarPresignResponse(
        String uploadUrl,
        String objectKey,
        long maxSizeBytes
) {}

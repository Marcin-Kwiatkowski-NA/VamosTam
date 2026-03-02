package com.vamigo.user.dto;

public record AvatarPresignResponse(
        String uploadUrl,
        String objectKey,
        long maxSizeBytes
) {}

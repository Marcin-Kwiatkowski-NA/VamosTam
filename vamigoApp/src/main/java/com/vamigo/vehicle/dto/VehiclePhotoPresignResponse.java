package com.vamigo.vehicle.dto;

public record VehiclePhotoPresignResponse(
        String uploadUrl,
        String objectKey,
        long maxSizeBytes
) {}

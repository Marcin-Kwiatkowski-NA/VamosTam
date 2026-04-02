package com.vamigo.vehicle.dto;

import jakarta.validation.constraints.NotBlank;

public record VehiclePhotoConfirmRequest(
        @NotBlank String objectKey
) {}

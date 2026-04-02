package com.vamigo.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateCarrierProfileRequest(
        @Size(max = 200, message = "Company name cannot exceed 200 characters")
        String companyName,

        @Size(max = 500, message = "Website URL cannot exceed 500 characters")
        String websiteUrl,

        Boolean bookingEnabled
) {
}

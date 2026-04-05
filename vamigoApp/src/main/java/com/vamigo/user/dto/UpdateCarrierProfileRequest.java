package com.vamigo.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCarrierProfileRequest(
        @Size(max = 200, message = "Company name cannot exceed 200 characters")
        String companyName,

        @Size(max = 500, message = "Website URL cannot exceed 500 characters")
        String websiteUrl,

        Boolean bookingEnabled,

        @Size(min = 3, max = 100, message = "Slug must be between 3 and 100 characters")
        @Pattern(regexp = "^[a-z0-9][a-z0-9-]*[a-z0-9]$", message = "Slug must be lowercase alphanumeric with hyphens, not starting or ending with a hyphen")
        String slug
) {
}

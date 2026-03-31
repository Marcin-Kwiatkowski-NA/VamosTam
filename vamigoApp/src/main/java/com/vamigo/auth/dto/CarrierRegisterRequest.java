package com.vamigo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CarrierRegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        @NotBlank(message = "Company name is required")
        @Size(max = 200, message = "Company name cannot exceed 200 characters")
        String companyName,

        @NotBlank(message = "NIP is required")
        @Pattern(regexp = "^[0-9]{10}$", message = "NIP must be exactly 10 digits")
        String nip,

        @NotBlank(message = "Phone number is required")
        @Size(max = 20, message = "Phone number cannot exceed 20 characters")
        String phoneNumber,

        @Size(max = 500, message = "Website URL cannot exceed 500 characters")
        String websiteUrl
) {
}

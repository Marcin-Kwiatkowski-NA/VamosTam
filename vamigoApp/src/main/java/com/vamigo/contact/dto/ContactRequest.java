package com.vamigo.contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 20) String phone,
        @NotBlank @Size(max = 2000) String message,
        @Size(max = 30) String appVersion,
        @Size(max = 30) String platform,
        @Size(max = 10) String locale
) {}

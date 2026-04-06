package com.vamigo.user.dto;

public record CarrierProfileDto(
        Long userId,
        String companyName,
        String nip,
        String websiteUrl,
        boolean bookingEnabled,
        String slug,
        String phoneNumber
) {
}

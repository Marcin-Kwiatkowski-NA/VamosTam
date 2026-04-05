package com.vamigo.user;

import com.vamigo.auth.AppPrincipal;
import com.vamigo.user.dto.CarrierProfileDto;
import com.vamigo.user.dto.UpdateCarrierProfileRequest;
import com.vamigo.user.exception.DuplicateSlugException;
import com.vamigo.user.exception.NoSuchUserException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/carrier-profile")
@RequiredArgsConstructor
public class CarrierProfileController {

    private final CarrierProfileRepository carrierProfileRepository;
    private final UserProfileRepository userProfileRepository;

    @GetMapping("/{userId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<CarrierProfileDto> getCarrierProfile(@PathVariable Long userId) {
        CarrierProfile carrier = carrierProfileRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));

        return ResponseEntity.ok(toDto(carrier));
    }

    @PatchMapping
    @PreAuthorize("hasRole('CARRIER')")
    public ResponseEntity<CarrierProfileDto> updateCarrierProfile(
            @AuthenticationPrincipal AppPrincipal principal,
            @Valid @RequestBody UpdateCarrierProfileRequest request) {

        CarrierProfile carrier = carrierProfileRepository.findById(principal.userId())
                .orElseThrow(() -> new NoSuchUserException(principal.userId()));

        if (request.companyName() != null && !request.companyName().isBlank()) {
            carrier.setCompanyName(request.companyName());

            UserProfile profile = userProfileRepository.findById(principal.userId())
                    .orElseThrow(() -> new NoSuchUserException(principal.userId()));
            profile.setDisplayName(request.companyName());
            userProfileRepository.save(profile);
        }
        if (request.websiteUrl() != null) {
            carrier.setWebsiteUrl(request.websiteUrl().isBlank() ? null : request.websiteUrl());
        }
        if (request.bookingEnabled() != null) {
            carrier.setBookingEnabled(request.bookingEnabled());
        }
        if (request.slug() != null && !request.slug().isBlank()) {
            String normalizedSlug = request.slug().toLowerCase();
            if (SlugUtils.isReserved(normalizedSlug)) {
                throw new IllegalArgumentException("This slug is reserved");
            }
            if (!normalizedSlug.equals(carrier.getSlug())
                    && carrierProfileRepository.existsBySlug(normalizedSlug)) {
                throw new DuplicateSlugException(normalizedSlug);
            }
            carrier.setSlug(normalizedSlug);
        }

        carrierProfileRepository.save(carrier);

        return ResponseEntity.ok(toDto(carrier));
    }

    private CarrierProfileDto toDto(CarrierProfile carrier) {
        return new CarrierProfileDto(
                carrier.getId(),
                carrier.getCompanyName(),
                carrier.getNip(),
                carrier.getWebsiteUrl(),
                carrier.isBookingEnabled(),
                carrier.getSlug()
        );
    }
}

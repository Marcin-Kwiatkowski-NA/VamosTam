package com.blablatwo.domain;

import com.blablatwo.config.DataInitializer;
import com.blablatwo.exceptions.DuplicateExternalEntityException;
import com.blablatwo.exceptions.FacebookBotMissingException;
import com.blablatwo.location.Location;
import com.blablatwo.location.LocationResolutionService;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;

@Component
public class ExternalImportSupport {

    private final LocationResolutionService locationResolutionService;
    private final UserAccountRepository userAccountRepository;

    public ExternalImportSupport(LocationResolutionService locationResolutionService,
                                  UserAccountRepository userAccountRepository) {
        this.locationResolutionService = locationResolutionService;
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * Deduplication relies solely on externalId, which is a SHA-256 fingerprint
     * computed by the scraper from: normalize(author)|normalize(origin)|normalize(destination)|formatDate(date)
     * where normalize() trims + lowercases (Polish locale) and formatDate() uses ISO YYYY-MM-DD.
     * This is group-independent by design — cross-posted rides across different Facebook groups
     * produce the same externalId, so the API returns HTTP 409 for duplicates.
     */
    public void validateNotDuplicate(String externalId, Function<String, Boolean> existsByExternalIdFn) {
        if (externalId != null && !externalId.isBlank() && existsByExternalIdFn.apply(externalId)) {
            throw new DuplicateExternalEntityException("Duplicate by externalId: " + externalId);
        }
    }

    public record ResolvedLocations(Location origin, Location destination) {
    }

    public ResolvedLocations resolveLocations(String originName, String destinationName) {
        var origin = locationResolutionService.resolveByName(originName);
        var destination = locationResolutionService.resolveByName(destinationName);
        return new ResolvedLocations(origin, destination);
    }

    public Optional<Location> tryResolveLocationByName(String locationName) {
        return locationResolutionService.tryResolveByName(locationName);
    }

    public UserAccount resolveProxyUser() {
        return userAccountRepository.findByEmail(DataInitializer.FACEBOOK_BOT_EMAIL)
                .orElseThrow(FacebookBotMissingException::new);
    }
}

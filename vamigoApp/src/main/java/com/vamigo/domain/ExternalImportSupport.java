package com.vamigo.domain;

import com.vamigo.config.DataInitializer;
import com.vamigo.exceptions.DepartureTooSoonException;
import com.vamigo.exceptions.DuplicateExternalEntityException;
import com.vamigo.exceptions.FacebookBotMissingException;
import com.vamigo.location.Location;
import com.vamigo.location.LocationResolutionService;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    private static final int MIN_DEPARTURE_NOTICE_MINUTES = 30;

    public void validateDepartureInFuture(Instant departureTime) {
        Instant earliest = Instant.now().plus(MIN_DEPARTURE_NOTICE_MINUTES, ChronoUnit.MINUTES);
        if (departureTime.isBefore(earliest)) {
            throw new DepartureTooSoonException(
                    "Departure must be at least " + MIN_DEPARTURE_NOTICE_MINUTES + " minutes from now");
        }
    }

    public UserAccount resolveProxyUser() {
        return userAccountRepository.findByEmail(DataInitializer.FACEBOOK_BOT_EMAIL)
                .orElseThrow(FacebookBotMissingException::new);
    }
}

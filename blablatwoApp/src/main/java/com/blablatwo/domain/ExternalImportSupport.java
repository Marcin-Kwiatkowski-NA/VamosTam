package com.blablatwo.domain;

import com.blablatwo.city.CityResolutionService;
import com.blablatwo.config.DataInitializer;
import com.blablatwo.exceptions.DuplicateExternalEntityException;
import com.blablatwo.exceptions.FacebookBotMissingException;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class ExternalImportSupport {

    private final CityResolutionService cityResolutionService;
    private final UserAccountRepository userAccountRepository;

    public ExternalImportSupport(CityResolutionService cityResolutionService,
                                  UserAccountRepository userAccountRepository) {
        this.cityResolutionService = cityResolutionService;
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

    public Segment resolveSegment(String originCityName, String destinationCityName, String langCode) {
        var origin = cityResolutionService.resolveCityByName(originCityName, langCode);
        var destination = cityResolutionService.resolveCityByName(destinationCityName, langCode);
        return new Segment(origin, destination);
    }

    public UserAccount resolveProxyUser() {
        return userAccountRepository.findByEmail(DataInitializer.FACEBOOK_BOT_EMAIL)
                .orElseThrow(FacebookBotMissingException::new);
    }
}

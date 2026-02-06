package com.blablatwo.domain;

import com.blablatwo.city.CityResolutionService;
import com.blablatwo.config.DataInitializer;
import com.blablatwo.exceptions.DuplicateExternalEntityException;
import com.blablatwo.exceptions.FacebookBotMissingException;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Function;

@Component
public class ExternalImportSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalImportSupport.class);

    private final CityResolutionService cityResolutionService;
    private final UserAccountRepository userAccountRepository;

    public ExternalImportSupport(CityResolutionService cityResolutionService,
                                  UserAccountRepository userAccountRepository) {
        this.cityResolutionService = cityResolutionService;
        this.userAccountRepository = userAccountRepository;
    }

    public String computeHash(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawContent.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("SHA-256 algorithm not available", e);
            return null;
        }
    }

    public void validateAndDeduplicate(String externalId, String contentHash,
                                        Function<String, Boolean> existsByExternalIdFn,
                                        Function<String, Boolean> existsByHashFn) {
        if (contentHash != null && !contentHash.isBlank() && existsByHashFn.apply(contentHash)) {
            throw new DuplicateExternalEntityException("Duplicate by contentHash");
        }
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

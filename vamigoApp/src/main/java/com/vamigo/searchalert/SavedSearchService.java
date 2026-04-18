package com.vamigo.searchalert;

import com.vamigo.searchalert.dto.*;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import com.vamigo.user.exception.NoSuchUserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@EnableConfigurationProperties(SearchAlertProperties.class)
public class SavedSearchService {

    private static final Logger log = LoggerFactory.getLogger(SavedSearchService.class);

    private final SavedSearchRepository savedSearchRepository;
    private final SearchAlertMatchRepository matchRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserAccountRepository userAccountRepository;
    private final SearchAlertProperties properties;

    public SavedSearchService(SavedSearchRepository savedSearchRepository,
                              SearchAlertMatchRepository matchRepository,
                              NotificationPreferenceRepository preferenceRepository,
                              UserAccountRepository userAccountRepository,
                              SearchAlertProperties properties) {
        this.savedSearchRepository = savedSearchRepository;
        this.matchRepository = matchRepository;
        this.preferenceRepository = preferenceRepository;
        this.userAccountRepository = userAccountRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<SavedSearchResponseDto> getMyAlerts(Long userId) {
        return savedSearchRepository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public SavedSearchResponseDto createAlert(Long userId, CreateSavedSearchRequest request) {
        long manualCount = savedSearchRepository.countByUserIdAndActiveTrueAndAutoCreatedFalse(userId);
        if (manualCount >= properties.maxAlertsPerUser()) {
            throw new IllegalStateException("Maximum number of search alerts (" + properties.maxAlertsPerUser() + ") reached");
        }

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));

        String label = request.originName() + " → " + request.destinationName()
                + " (" + request.departureDate() + ")";

        SavedSearch saved = savedSearchRepository.save(SavedSearch.builder()
                .user(user)
                .originOsmId(request.originOsmId())
                .originName(request.originName())
                .originLat(request.originLat())
                .originLon(request.originLon())
                .destinationOsmId(request.destinationOsmId())
                .destinationName(request.destinationName())
                .destinationLat(request.destinationLat())
                .destinationLon(request.destinationLon())
                .searchType(request.searchType())
                .departureDate(request.departureDate())
                .minAvailableSeats(request.minAvailableSeats())
                .label(label)
                .autoCreated(false)
                .build());

        return toDto(saved);
    }

    @Transactional
    public SavedSearch createAutoAlert(Long userId, String originName, Long originOsmId,
                                       double originLat, double originLon,
                                       String destName, Long destOsmId,
                                       double destLat, double destLon,
                                       LocalDate departureDate, SearchType searchType) {
        boolean exists = savedSearchRepository.existsActiveSearch(
                userId, originOsmId, destOsmId, departureDate, searchType);
        if (exists) {
            log.debug("Auto-alert already exists for user {} route {}->{} on {}", userId, originOsmId, destOsmId, departureDate);
            return null;
        }

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));

        String label = originName + " → " + destName + " (" + departureDate + ")";

        return savedSearchRepository.save(SavedSearch.builder()
                .user(user)
                .originOsmId(originOsmId)
                .originName(originName)
                .originLat(originLat)
                .originLon(originLon)
                .destinationOsmId(destOsmId)
                .destinationName(destName)
                .destinationLat(destLat)
                .destinationLon(destLon)
                .searchType(searchType)
                .departureDate(departureDate)
                .label(label)
                .autoCreated(true)
                .build());
    }

    @Transactional
    public void deleteAlert(Long userId, Long alertId) {
        SavedSearch alert = savedSearchRepository.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        matchRepository.deleteBySavedSearchId(alertId);
        savedSearchRepository.delete(alert);
    }

    @Transactional
    public SavedSearchResponseDto toggleAlert(Long userId, Long alertId, boolean active) {
        SavedSearch alert = savedSearchRepository.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        alert.setActive(active);
        return toDto(alert);
    }

    @Transactional
    public NotificationPreference getOrCreatePreferences(Long userId) {
        return preferenceRepository.findById(userId).orElseGet(() -> {
            UserAccount user = userAccountRepository.findById(userId)
                    .orElseThrow(() -> new NoSuchUserException(userId));
            return preferenceRepository.save(NotificationPreference.builder()
                    .user(user)
                    .build());
        });
    }

    @Transactional(readOnly = true)
    public NotificationPreferenceDto getPreferencesDto(Long userId) {
        NotificationPreference pref = getOrCreatePreferences(userId);
        return new NotificationPreferenceDto(pref.isSearchAlertsPushEnabled(), pref.isSearchAlertsEmailEnabled());
    }

    @Transactional
    public NotificationPreferenceDto updatePreferences(Long userId, UpdateNotificationPreferenceRequest request) {
        NotificationPreference pref = getOrCreatePreferences(userId);
        if (request.searchAlertsPushEnabled() != null) {
            pref.setSearchAlertsPushEnabled(request.searchAlertsPushEnabled());
        }
        if (request.searchAlertsEmailEnabled() != null) {
            pref.setSearchAlertsEmailEnabled(request.searchAlertsEmailEnabled());
        }
        return new NotificationPreferenceDto(pref.isSearchAlertsPushEnabled(), pref.isSearchAlertsEmailEnabled());
    }

    @Transactional
    public void unsubscribeByToken(String token) {
        preferenceRepository.findByUnsubscribeToken(token).ifPresent(pref -> {
            pref.setSearchAlertsEmailEnabled(false);
            log.info("User {} unsubscribed from search alert emails via token", pref.getUserId());
        });
    }

    private SavedSearchResponseDto toDto(SavedSearch ss) {
        return SavedSearchResponseDto.builder()
                .id(ss.getId())
                .label(ss.getLabel())
                .originName(ss.getOriginName())
                .destinationName(ss.getDestinationName())
                .originOsmId(ss.getOriginOsmId())
                .originLat(ss.getOriginLat())
                .originLon(ss.getOriginLon())
                .destinationOsmId(ss.getDestinationOsmId())
                .destinationLat(ss.getDestinationLat())
                .destinationLon(ss.getDestinationLon())
                .departureDate(ss.getDepartureDate())
                .searchType(ss.getSearchType())
                .minAvailableSeats(ss.getMinAvailableSeats())
                .autoCreated(ss.isAutoCreated())
                .active(ss.isActive())
                .createdAt(ss.getCreatedAt())
                .lastPushSentAt(ss.getLastPushSentAt())
                .lastEmailSentAt(ss.getLastEmailSentAt())
                .build();
    }
}

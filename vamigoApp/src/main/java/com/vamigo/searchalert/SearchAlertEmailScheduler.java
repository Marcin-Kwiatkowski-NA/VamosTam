package com.vamigo.searchalert;

import com.vamigo.email.BrevoClient;
import com.vamigo.user.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SearchAlertEmailScheduler {

    private static final Logger log = LoggerFactory.getLogger(SearchAlertEmailScheduler.class);

    private final SearchAlertMatchRepository matchRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final SavedSearchService savedSearchService;
    private final BrevoClient brevoClient;
    private final long templateId;
    private final long templateIdPl;

    public SearchAlertEmailScheduler(SearchAlertMatchRepository matchRepository,
                                     NotificationPreferenceRepository preferenceRepository,
                                     SavedSearchService savedSearchService,
                                     BrevoClient brevoClient,
                                     @Value("${brevo.search-alert-digest-template-id}") long templateId,
                                     @Value("${brevo.search-alert-digest-template-id-pl}") long templateIdPl) {
        this.matchRepository = matchRepository;
        this.preferenceRepository = preferenceRepository;
        this.savedSearchService = savedSearchService;
        this.brevoClient = brevoClient;
        this.templateId = templateId;
        this.templateIdPl = templateIdPl;
    }

    @Scheduled(cron = "${search-alert.email-cron}")
    @Transactional
    public void drainEmailOutbox() {
        List<SearchAlertMatch> unsentMatches = matchRepository.findUnsentEmail();
        if (unsentMatches.isEmpty()) return;

        // Group by saved search
        Map<Long, List<SearchAlertMatch>> bySearch = unsentMatches.stream()
                .collect(Collectors.groupingBy(m -> m.getSavedSearch().getId(), LinkedHashMap::new, Collectors.toList()));

        // Group saved searches by user for digest
        Map<Long, List<Map.Entry<Long, List<SearchAlertMatch>>>> byUser = new LinkedHashMap<>();
        for (var entry : bySearch.entrySet()) {
            SavedSearch ss = entry.getValue().getFirst().getSavedSearch();
            byUser.computeIfAbsent(ss.getUser().getId(), k -> new ArrayList<>()).add(entry);
        }

        Instant now = Instant.now();
        List<Long> toDelete = new ArrayList<>();

        for (var userEntry : byUser.entrySet()) {
            Long userId = userEntry.getKey();

            // Check user preference
            NotificationPreference pref = preferenceRepository.findById(userId).orElse(null);
            if (pref != null && !pref.isSearchAlertsEmailEnabled()) {
                markEmailSentAndCollectDeletable(userEntry.getValue(), toDelete, now);
                continue;
            }

            // Build digest data for all alerts of this user
            List<Map<String, String>> alertItems = new ArrayList<>();
            UserAccount user = null;
            String unsubscribeToken = null;

            for (var searchEntry : userEntry.getValue()) {
                List<SearchAlertMatch> matches = searchEntry.getValue();
                SavedSearch ss = matches.getFirst().getSavedSearch();

                if (!ss.isActive()) continue;

                user = ss.getUser();
                if (unsubscribeToken == null) {
                    NotificationPreference userPref = savedSearchService.getOrCreatePreferences(userId);
                    unsubscribeToken = userPref.getUnsubscribeToken();
                }

                alertItems.add(Map.of(
                        "ROUTE", ss.getLabel(),
                        "MATCH_COUNT", String.valueOf(matches.size()),
                        "DEPARTURE_DATE", ss.getDepartureDate().toString(),
                        "DEEP_LINK", "/rides/list?originOsmId=" + ss.getOriginOsmId()
                                + "&destinationOsmId=" + ss.getDestinationOsmId()
                                + "&originName=" + ss.getOriginName()
                                + "&destinationName=" + ss.getDestinationName()
                                + "&earliestDeparture=" + ss.getDepartureDate()
                ));
            }

            if (alertItems.isEmpty() || user == null) {
                markEmailSentAndCollectDeletable(userEntry.getValue(), toDelete, now);
                continue;
            }

            try {
                Map<String, String> params = new LinkedHashMap<>();
                params.put("ALERT_COUNT", String.valueOf(alertItems.size()));
                // For simplicity, put first alert data + total count
                params.put("ROUTE", alertItems.getFirst().get("ROUTE"));
                params.put("MATCH_COUNT", alertItems.stream()
                        .mapToInt(a -> Integer.parseInt(a.get("MATCH_COUNT")))
                        .sum() + "");
                params.put("UNSUBSCRIBE_LINK", "/public/search-alerts/unsubscribe?token=" + unsubscribeToken);

                brevoClient.sendTemplateEmail(
                        user.getEmail(),
                        user.getEmail(),
                        templateIdPl,
                        params);

                log.info("Search alert digest email sent to user {}", userId);
            } catch (Exception e) {
                log.error("Failed to send search alert digest email to user {}: {}", userId, e.getMessage());
            }

            markEmailSentAndCollectDeletable(userEntry.getValue(), toDelete, now);
        }

        if (!toDelete.isEmpty()) {
            matchRepository.deleteByIds(toDelete);
        }

        log.debug("Email scheduler processed {} users, deleted {} outbox rows", byUser.size(), toDelete.size());
    }

    private void markEmailSentAndCollectDeletable(
            List<Map.Entry<Long, List<SearchAlertMatch>>> searchEntries,
            List<Long> toDelete, Instant now) {
        for (var searchEntry : searchEntries) {
            SavedSearch ss = searchEntry.getValue().getFirst().getSavedSearch();
            ss.setLastEmailSentAt(now);
            for (SearchAlertMatch m : searchEntry.getValue()) {
                m.setEmailSent(true);
                if (m.isPushSent()) {
                    toDelete.add(m.getId());
                }
            }
        }
    }
}

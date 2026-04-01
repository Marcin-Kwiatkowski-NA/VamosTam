package com.vamigo.searchalert;

import com.vamigo.notification.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@EnableConfigurationProperties(SearchAlertProperties.class)
public class SearchAlertPushScheduler {

    private static final Logger log = LoggerFactory.getLogger(SearchAlertPushScheduler.class);

    private final SearchAlertMatchRepository matchRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationService notificationService;
    private final SearchAlertProperties properties;

    public SearchAlertPushScheduler(SearchAlertMatchRepository matchRepository,
                                    NotificationPreferenceRepository preferenceRepository,
                                    NotificationService notificationService,
                                    SearchAlertProperties properties) {
        this.matchRepository = matchRepository;
        this.preferenceRepository = preferenceRepository;
        this.notificationService = notificationService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${search-alert.push-check-interval-ms}")
    @Transactional
    public void drainPushOutbox() {
        List<SearchAlertMatch> unsentMatches = matchRepository.findUnsentPush();
        if (unsentMatches.isEmpty()) return;

        // Group by saved search
        Map<Long, List<SearchAlertMatch>> grouped = unsentMatches.stream()
                .collect(Collectors.groupingBy(m -> m.getSavedSearch().getId(), LinkedHashMap::new, Collectors.toList()));

        Instant now = Instant.now();
        List<Long> toDelete = new ArrayList<>();

        for (var entry : grouped.entrySet()) {
            List<SearchAlertMatch> matches = entry.getValue();
            SavedSearch ss = matches.getFirst().getSavedSearch();

            // Auto-expire past departure date
            if (ss.getDepartureDate().isBefore(LocalDate.now())) {
                ss.setActive(false);
                markPushSentAndCollectDeletable(matches, toDelete);
                continue;
            }

            if (!ss.isActive()) {
                markPushSentAndCollectDeletable(matches, toDelete);
                continue;
            }

            // Enforce push cadence
            if (ss.getLastPushSentAt() != null
                    && ss.getLastPushSentAt().plus(properties.pushIntervalHours(), ChronoUnit.HOURS).isAfter(now)) {
                continue; // Not yet time — skip, don't mark sent
            }

            // Check user preference
            NotificationPreference pref = preferenceRepository.findById(ss.getUser().getId()).orElse(null);
            if (pref != null && !pref.isSearchAlertsPushEnabled()) {
                markPushSentAndCollectDeletable(matches, toDelete);
                continue;
            }

            int matchCount = matches.size();

            // Build deep link with search criteria pre-filled
            String deepLink = "/rides/list?originOsmId=" + ss.getOriginOsmId()
                    + "&destinationOsmId=" + ss.getDestinationOsmId()
                    + "&originName=" + ss.getOriginName()
                    + "&destinationName=" + ss.getDestinationName()
                    + "&originLat=" + ss.getOriginLat()
                    + "&originLon=" + ss.getOriginLon()
                    + "&destinationLat=" + ss.getDestinationLat()
                    + "&destinationLon=" + ss.getDestinationLon()
                    + "&earliestDeparture=" + ss.getDepartureDate();

            var params = Map.of(
                    "origin", ss.getOriginName(),
                    "destination", ss.getDestinationName(),
                    "matchCount", String.valueOf(matchCount),
                    "deepLink", deepLink
            );

            notificationService.notify(NotificationRequest.builder()
                    .recipientId(ss.getUser().getId())
                    .type(NotificationType.SEARCH_ALERT_MATCH)
                    .entityType(EntityType.SAVED_SEARCH)
                    .entityId(ss.getId().toString())
                    .params(params)
                    .collapseKey("search-alert:" + ss.getId())
                    .build());

            ss.setLastPushSentAt(now);
            markPushSentAndCollectDeletable(matches, toDelete);
        }

        if (!toDelete.isEmpty()) {
            matchRepository.deleteByIds(toDelete);
        }

        log.debug("Push scheduler processed {} groups, deleted {} outbox rows", grouped.size(), toDelete.size());
    }

    private void markPushSentAndCollectDeletable(List<SearchAlertMatch> matches, List<Long> toDelete) {
        for (SearchAlertMatch m : matches) {
            m.setPushSent(true);
            // Delete if: push sent AND (email already sent OR not exact match — proximity-only rows don't need email)
            if (m.isEmailSent() || !m.isExactMatch()) {
                toDelete.add(m.getId());
            }
        }
    }
}

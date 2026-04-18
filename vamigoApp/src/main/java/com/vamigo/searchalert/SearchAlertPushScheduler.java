package com.vamigo.searchalert;

import com.vamigo.domain.PersonDisplayNameResolver;
import com.vamigo.notification.*;
import com.vamigo.ride.Ride;
import com.vamigo.ride.RideRepository;
import com.vamigo.seat.Seat;
import com.vamigo.seat.SeatRepository;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserProfile;
import com.vamigo.user.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
    private final NotificationParamsEnricher enricher;
    private final SearchAlertProperties properties;
    private final RideRepository rideRepository;
    private final SeatRepository seatRepository;
    private final UserProfileRepository userProfileRepository;
    private final PersonDisplayNameResolver displayNameResolver;

    public SearchAlertPushScheduler(SearchAlertMatchRepository matchRepository,
                                    NotificationPreferenceRepository preferenceRepository,
                                    NotificationService notificationService,
                                    NotificationParamsEnricher enricher,
                                    SearchAlertProperties properties,
                                    RideRepository rideRepository,
                                    SeatRepository seatRepository,
                                    UserProfileRepository userProfileRepository,
                                    PersonDisplayNameResolver displayNameResolver) {
        this.matchRepository = matchRepository;
        this.preferenceRepository = preferenceRepository;
        this.notificationService = notificationService;
        this.enricher = enricher;
        this.properties = properties;
        this.rideRepository = rideRepository;
        this.seatRepository = seatRepository;
        this.userProfileRepository = userProfileRepository;
        this.displayNameResolver = displayNameResolver;
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

            // Split matches by kind — emit separate notifications for rides vs seats
            List<SearchAlertMatch> rideMatches = matches.stream()
                    .filter(m -> m.getRideId() != null)
                    .toList();
            List<SearchAlertMatch> seatMatches = matches.stream()
                    .filter(m -> m.getSeatId() != null)
                    .toList();

            if (!rideMatches.isEmpty()) {
                emitKindNotification(ss, rideMatches, ResultKind.RIDE);
            }
            if (!seatMatches.isEmpty()) {
                emitKindNotification(ss, seatMatches, ResultKind.SEAT);
            }

            ss.setLastPushSentAt(now);
            markPushSentAndCollectDeletable(matches, toDelete);
        }

        if (!toDelete.isEmpty()) {
            matchRepository.deleteByIds(toDelete);
        }

        log.debug("Push scheduler processed {} groups, deleted {} outbox rows", grouped.size(), toDelete.size());
    }

    private void emitKindNotification(SavedSearch ss, List<SearchAlertMatch> matches, ResultKind kind) {
        int matchCount = matches.size();
        EntityType entityType = kind == ResultKind.SEAT ? EntityType.SEAT : EntityType.RIDE;
        List<NotificationParamsEnricher.SearchAlertMatchInfo> matchInfos =
                buildMatchInfos(matches, kind);

        if (matchCount == 1) {
            long id = kind == ResultKind.SEAT
                    ? matches.getFirst().getSeatId()
                    : matches.getFirst().getRideId();
            String deepLink = enricher.buildSearchAlertEntityDeepLink(kind, id);

            Map<String, String> params = new LinkedHashMap<>();
            params.put("origin", ss.getOriginName());
            params.put("destination", ss.getDestinationName());
            params.put("matchCount", "1");
            params.put("deepLink", deepLink);
            params.putAll(enricher.enrichSearchAlertSingle(
                    ss, matchInfos.isEmpty() ? null : matchInfos.getFirst()));

            notificationService.notify(NotificationRequest.builder()
                    .recipientId(ss.getUser().getId())
                    .type(NotificationType.SEARCH_ALERT_MATCH)
                    .entityType(entityType)
                    .entityId(String.valueOf(id))
                    .targetType(TargetType.ENTITY)
                    .params(params)
                    .collapseKey("search-alert:" + ss.getId() + ":" + kind.name().toLowerCase() + ":" + id)
                    .build());
            return;
        }

        String listRoutePrefix = kind == ResultKind.SEAT ? "/seats/list" : "/rides/list";
        var listFilters = enricher.buildSearchAlertListFilters(ss, listRoutePrefix);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("origin", ss.getOriginName());
        params.put("destination", ss.getDestinationName());
        params.put("matchCount", String.valueOf(matchCount));
        params.put("deepLink", listFilters.deepLink());
        params.putAll(enricher.enrichSearchAlertMulti(ss, matchInfos));

        notificationService.notify(NotificationRequest.builder()
                .recipientId(ss.getUser().getId())
                .type(NotificationType.SEARCH_ALERT_MATCH)
                .entityType(EntityType.SAVED_SEARCH)
                .entityId(ss.getId().toString())
                .targetType(TargetType.LIST)
                .resultKind(kind)
                .listFilters(listFilters.filters())
                .params(params)
                .collapseKey("search-alert:" + ss.getId() + ":" + kind.name().toLowerCase())
                .build());
    }

    /**
     * Hydrate the rich preview info (departure time, counterparty name, price)
     * for the matches that survived filtering. Sorted ascending by departure
     * so the multi-match enricher can pick {@code earliestDeparture} and the
     * preview rows render in chronological order.
     */
    private List<NotificationParamsEnricher.SearchAlertMatchInfo> buildMatchInfos(
            List<SearchAlertMatch> matches, ResultKind kind) {
        if (matches.isEmpty()) return List.of();

        List<Long> ids = new ArrayList<>(matches.size());
        for (var m : matches) {
            Long id = kind == ResultKind.SEAT ? m.getSeatId() : m.getRideId();
            if (id != null) ids.add(id);
        }
        if (ids.isEmpty()) return List.of();

        List<NotificationParamsEnricher.SearchAlertMatchInfo> infos = new ArrayList<>();
        if (kind == ResultKind.SEAT) {
            var seats = seatRepository.findAllById(ids);
            // Resolve passenger names in one pass to avoid N+1.
            Map<Long, String> nameById = resolveDisplayNames(
                    seats.stream().map(Seat::getPassenger).toList());
            for (var s : seats) {
                infos.add(new NotificationParamsEnricher.SearchAlertMatchInfo(
                        s.getDepartureTime(),
                        nameById.get(s.getPassenger() != null ? s.getPassenger().getId() : null),
                        s.getPriceWillingToPay()));
            }
        } else {
            var rides = rideRepository.findAllById(ids);
            Map<Long, String> nameById = resolveDisplayNames(
                    rides.stream().map(Ride::getDriver).toList());
            for (var r : rides) {
                infos.add(new NotificationParamsEnricher.SearchAlertMatchInfo(
                        r.getDepartureTime(),
                        nameById.get(r.getDriver() != null ? r.getDriver().getId() : null),
                        r.getPricePerSeat()));
            }
        }
        infos.sort(Comparator.comparing(
                NotificationParamsEnricher.SearchAlertMatchInfo::departureTime,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return infos;
    }

    private Map<Long, String> resolveDisplayNames(List<UserAccount> users) {
        Map<Long, String> result = new HashMap<>();
        if (users == null || users.isEmpty()) return result;
        List<Long> userIds = users.stream()
                .filter(java.util.Objects::nonNull)
                .map(UserAccount::getId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, UserProfile> profiles = userProfileRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getId, p -> p, (a, b) -> a));
        for (Long uid : userIds) {
            String name = displayNameResolver.resolveInternal(profiles.get(uid), uid);
            if (name != null) result.put(uid, name);
        }
        return result;
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

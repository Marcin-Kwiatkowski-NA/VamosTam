package com.vamigo.searchalert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Component
public class SavedSearchExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(SavedSearchExpiryScheduler.class);

    private final SavedSearchRepository repository;
    private final Clock clock;

    public SavedSearchExpiryScheduler(SavedSearchRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Scheduled(cron = "${search-alert.expiry-cron}", zone = "${search-alert.expiry-zone}")
    @Transactional
    public void deactivateExpiredSearches() {
        int updated = repository.deactivateExpired(LocalDate.now(clock));
        if (updated > 0) {
            log.info("Deactivated {} saved searches past their departure date", updated);
        }
    }
}

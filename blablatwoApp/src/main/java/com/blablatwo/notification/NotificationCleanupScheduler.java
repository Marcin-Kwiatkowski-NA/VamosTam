package com.blablatwo.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Deletes read notifications older than 90 days.
 * Unread notifications are kept indefinitely.
 */
@Component
public class NotificationCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationCleanupScheduler.class);
    private static final int RETENTION_DAYS = 90;

    private final NotificationRepository notificationRepository;

    public NotificationCleanupScheduler(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanup() {
        Instant cutoff = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);
        int deleted = notificationRepository.deleteReadOlderThan(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} read notifications older than {} days", deleted, RETENTION_DAYS);
        }
    }
}

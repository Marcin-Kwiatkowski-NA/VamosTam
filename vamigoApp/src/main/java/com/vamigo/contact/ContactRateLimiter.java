package com.vamigo.contact;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ContactRateLimiter {

    private static final int MAX_SUBMISSIONS_PER_HOUR = 3;

    private final Cache<Long, AtomicInteger> submissionCounts = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .maximumSize(10_000)
            .build();

    public void checkAndRecord(Long userId) {
        AtomicInteger count = submissionCounts.get(userId, _ -> new AtomicInteger(0));
        if (count.incrementAndGet() > MAX_SUBMISSIONS_PER_HOUR) {
            throw new ContactRateLimitException();
        }
    }
}

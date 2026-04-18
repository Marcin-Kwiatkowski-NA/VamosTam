package com.vamigo.searchalert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavedSearchExpirySchedulerTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    @Mock
    private SavedSearchRepository repository;

    @Test
    void passesLocalWarsawDateToRepository() {
        // 22:10 UTC on Apr 18 is 00:10 Apr 19 in Warsaw — the zone-aware clock must read Apr 19.
        Clock clock = Clock.fixed(Instant.parse("2026-04-18T22:10:00Z"), WARSAW);
        SavedSearchExpiryScheduler scheduler = new SavedSearchExpiryScheduler(repository, clock);
        when(repository.deactivateExpired(LocalDate.of(2026, 4, 19))).thenReturn(3);

        scheduler.deactivateExpiredSearches();

        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(repository).deactivateExpired(captor.capture());
        assertThat(captor.getValue()).isEqualTo(LocalDate.of(2026, 4, 19));
    }
}

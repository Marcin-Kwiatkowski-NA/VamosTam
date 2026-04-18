package com.vamigo.match;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class MatchScoreTest {

    @Test
    @DisplayName("exact() carries null distances and exactMatch=true")
    void exactFactoryNullsDistances() {
        MatchScore score = MatchScore.exact();

        assertThat(score.originDistanceM()).isNull();
        assertThat(score.destinationDistanceM()).isNull();
        assertThat(score.exactMatch()).isTrue();
        assertThat(score.combinedM()).isZero();
    }

    @Test
    @DisplayName("proximity() sets both distances and exactMatch=false")
    void proximityFactorySetsDistances() {
        MatchScore score = MatchScore.proximity(1_000, 2_500);

        assertThat(score.originDistanceM()).isEqualTo(1_000);
        assertThat(score.destinationDistanceM()).isEqualTo(2_500);
        assertThat(score.exactMatch()).isFalse();
        assertThat(score.combinedM()).isEqualTo(3_500);
    }

    @Test
    @DisplayName("combinedM treats null side distances as zero")
    void combinedMCoalescesNullToZero() {
        assertThat(new MatchScore(null, 500, false).combinedM()).isEqualTo(500);
        assertThat(new MatchScore(500, null, false).combinedM()).isEqualTo(500);
        assertThat(new MatchScore(null, null, true).combinedM()).isZero();
    }

    @Test
    @DisplayName("Sorting by combinedM puts exact matches (0) before proximity matches")
    void sortingByCombined() {
        MatchScore far = MatchScore.proximity(10_000, 10_000);
        MatchScore near = MatchScore.proximity(100, 100);
        MatchScore exact = MatchScore.exact();

        List<MatchScore> sorted = List.of(far, near, exact).stream()
                .sorted((a, b) -> Integer.compare(a.combinedM(), b.combinedM()))
                .collect(Collectors.toList());

        assertThat(sorted).containsExactly(exact, near, far);
    }
}

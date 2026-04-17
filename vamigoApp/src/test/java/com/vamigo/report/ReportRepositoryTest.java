package com.vamigo.report;

import com.vamigo.AbstractIntegrationTest;
import com.vamigo.user.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static com.vamigo.util.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ReportRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ReportRepository repository;

    private UserAccount author;

    @BeforeEach
    void setUp() {
        author = em.persistAndFlush(anActiveUserAccount().build());
    }

    @Nested
    @DisplayName("Check whether an author has already reported a target")
    class ExistsTests {

        @Test
        @DisplayName("Returns true when author, target type and target id all match a report")
        void returnsTrueWhenAllThreeMatch() {
            em.persistAndFlush(aReport(author).targetType(ReportTargetType.RIDE).targetId(42L).build());
            em.clear();

            boolean exists = repository.existsByAuthorIdAndTargetTypeAndTargetId(
                    author.getId(), ReportTargetType.RIDE, 42L);

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Returns false when the author reported a different target type")
        void returnsFalseWhenTargetTypeDiffers() {
            em.persistAndFlush(aReport(author).targetType(ReportTargetType.RIDE).targetId(42L).build());
            em.clear();

            boolean exists = repository.existsByAuthorIdAndTargetTypeAndTargetId(
                    author.getId(), ReportTargetType.SEAT, 42L);

            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Returns false when the author reported a different target id")
        void returnsFalseWhenTargetIdDiffers() {
            em.persistAndFlush(aReport(author).targetType(ReportTargetType.RIDE).targetId(42L).build());
            em.clear();

            boolean exists = repository.existsByAuthorIdAndTargetTypeAndTargetId(
                    author.getId(), ReportTargetType.RIDE, 99L);

            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Returns false when a different author reported the same target")
        void returnsFalseWhenAuthorDiffers() {
            UserAccount other = em.persistAndFlush(
                    anActiveUserAccount().email("other@example.com").build());
            em.persistAndFlush(aReport(author).targetType(ReportTargetType.RIDE).targetId(42L).build());
            em.clear();

            boolean exists = repository.existsByAuthorIdAndTargetTypeAndTargetId(
                    other.getId(), ReportTargetType.RIDE, 42L);

            assertThat(exists).isFalse();
        }
    }
}

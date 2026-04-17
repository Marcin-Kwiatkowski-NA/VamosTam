package com.vamigo.messaging;

import com.vamigo.AbstractIntegrationTest;
import com.vamigo.user.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.vamigo.util.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class MessageRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private MessageRepository repository;

    private UserAccount alice;
    private UserAccount bob;
    private Conversation conv;

    @BeforeEach
    void setUp() {
        alice = em.persistAndFlush(anActiveUserAccount().email("alice@example.com").build());
        bob = em.persistAndFlush(anActiveUserAccount().email("bob@example.com").build());
        conv = em.persistAndFlush(aConversation(alice, bob).topicKey("RIDE:1").build());
    }

    @Nested
    @DisplayName("List messages in a conversation newest-first")
    class FindByConversationDescTests {

        @Test
        @DisplayName("Returns every message belonging to the conversation")
        void returnsAllMessagesForConversation() {
            em.persistAndFlush(aMessage(conv, alice).body("hi").build());
            em.persistAndFlush(aMessage(conv, bob).body("hey").build());
            em.clear();

            List<Message> result = repository.findByConversationIdOrderByCreatedAtDesc(
                    conv.getId(), PageRequest.of(0, 10));

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("List messages created before a cutoff (older page)")
    class FindBeforeTests {

        @Test
        @DisplayName("Returns only messages created before the cutoff instant")
        void returnsOnlyMessagesBeforeCutoff() {
            em.persistAndFlush(aMessage(conv, alice).body("old").build());
            em.flush();
            em.clear();

            Instant future = Instant.now().plus(1, ChronoUnit.HOURS);
            List<Message> result = repository.findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                    conv.getId(), future, PageRequest.of(0, 10));

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Excludes messages that were created at or after the cutoff")
        void excludesMessagesAtOrAfterCutoff() {
            em.persistAndFlush(aMessage(conv, alice).body("recent").build());
            em.clear();

            Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
            List<Message> result = repository.findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                    conv.getId(), past, PageRequest.of(0, 10));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("List messages created after a cutoff (newer page)")
    class FindAfterTests {

        @Test
        @DisplayName("Returns only messages created after the cutoff instant")
        void returnsOnlyMessagesAfterCutoff() {
            em.persistAndFlush(aMessage(conv, alice).body("recent").build());
            em.clear();

            Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
            List<Message> result = repository.findByConversationIdAndCreatedAtAfterOrderByCreatedAtAsc(
                    conv.getId(), past, PageRequest.of(0, 10));

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Mark messages as delivered to recipient")
    class MarkDeliveredTests {

        @Test
        @DisplayName("Marks undelivered messages from the other party and leaves own messages untouched")
        void marksUndeliveredMessagesFromSender() {
            Message m1 = em.persistAndFlush(aMessage(conv, alice).body("a").build());
            Message m2 = em.persistAndFlush(aMessage(conv, alice).body("b").build());
            Message fromOther = em.persistAndFlush(aMessage(conv, bob).body("c").build());
            em.clear();

            Instant now = Instant.now();
            int updated = repository.markDelivered(conv.getId(), alice.getId(),
                    Instant.now().plus(1, ChronoUnit.HOURS), now);

            assertThat(updated).isEqualTo(2);
            assertThat(em.find(Message.class, m1.getId()).getDeliveredAt()).isNotNull();
            assertThat(em.find(Message.class, m2.getId()).getDeliveredAt()).isNotNull();
            assertThat(em.find(Message.class, fromOther.getId()).getDeliveredAt()).isNull();
        }

        @Test
        @DisplayName("Updates nothing when every message is already marked delivered")
        void skipsMessagesAlreadyDelivered() {
            em.persistAndFlush(aMessage(conv, alice).deliveredAt(Instant.now()).build());
            em.clear();

            int updated = repository.markDelivered(conv.getId(), alice.getId(),
                    Instant.now().plus(1, ChronoUnit.HOURS), Instant.now());

            assertThat(updated).isZero();
        }
    }

    @Nested
    @DisplayName("Mark messages as read by recipient")
    class MarkReadTests {

        @Test
        @DisplayName("Sets readAt and backfills deliveredAt when it was still null")
        void marksReadAndBackfillsDelivered() {
            Message m = em.persistAndFlush(aMessage(conv, alice).body("a").build());
            em.clear();

            Instant now = Instant.now();
            int updated = repository.markRead(conv.getId(), alice.getId(),
                    Instant.now().plus(1, ChronoUnit.HOURS), now);

            assertThat(updated).isEqualTo(1);
            Message reloaded = em.find(Message.class, m.getId());
            assertThat(reloaded.getReadAt()).isNotNull();
            assertThat(reloaded.getDeliveredAt()).isNotNull();
        }
    }
}

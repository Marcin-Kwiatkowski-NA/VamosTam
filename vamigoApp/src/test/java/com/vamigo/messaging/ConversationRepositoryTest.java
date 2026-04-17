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
import java.util.Optional;

import static com.vamigo.util.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ConversationRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ConversationRepository repository;

    private UserAccount alice;
    private UserAccount bob;
    private UserAccount carol;

    @BeforeEach
    void setUp() {
        alice = em.persistAndFlush(anActiveUserAccount().email("alice@example.com").build());
        bob = em.persistAndFlush(anActiveUserAccount().email("bob@example.com").build());
        carol = em.persistAndFlush(anActiveUserAccount().email("carol@example.com").build());
    }

    @Nested
    @DisplayName("Lookup conversation by topic and both participants")
    class FindByTopicKeyAndParticipantsTests {

        @Test
        @DisplayName("Returns conversation when topic and both participants match")
        void returnsConversationWhenAllMatch() {
            Conversation conv = em.persistAndFlush(
                    aConversation(alice, bob).topicKey("RIDE:42").build());
            em.clear();

            Optional<Conversation> found = repository.findByTopicKeyAndParticipants(
                    "RIDE:42", alice.getId(), bob.getId());

            assertThat(found).isPresent()
                    .get().extracting(Conversation::getId).isEqualTo(conv.getId());
        }

        @Test
        @DisplayName("Returns empty when the topic key does not match an existing conversation")
        void returnsEmptyWhenTopicMismatch() {
            em.persistAndFlush(aConversation(alice, bob).topicKey("RIDE:1").build());
            em.clear();

            Optional<Conversation> found = repository.findByTopicKeyAndParticipants(
                    "RIDE:99", alice.getId(), bob.getId());

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("List conversations a user participates in")
    class FindByParticipantIdTests {

        @Test
        @DisplayName("Returns conversations where the user is either participant A or B")
        void returnsConversationsWhereUserIsEitherParticipant() {
            em.persistAndFlush(aConversation(alice, bob).topicKey("RIDE:1").build());
            em.persistAndFlush(aConversation(carol, alice).topicKey("RIDE:2").build());
            em.persistAndFlush(aConversation(bob, carol).topicKey("RIDE:3").build());
            em.clear();

            List<Conversation> result = repository.findByParticipantId(alice.getId(), PageRequest.of(0, 10));

            assertThat(result).hasSize(2)
                    .extracting(Conversation::getTopicKey)
                    .containsExactlyInAnyOrder("RIDE:1", "RIDE:2");
        }
    }

    @Nested
    @DisplayName("List a user's conversations updated after a cutoff")
    class FindByParticipantUpdatedAfterTests {

        @Test
        @DisplayName("Includes conversations updated after the given cutoff")
        void filtersByUpdatedAt() {
            em.persistAndFlush(aConversation(alice, bob).topicKey("RIDE:1").build());
            em.clear();

            Instant past = Instant.now().minus(1, ChronoUnit.DAYS);
            List<Conversation> result = repository.findByParticipantIdAndUpdatedAtAfter(
                    alice.getId(), past, PageRequest.of(0, 10));

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Excludes conversations that were not updated after the cutoff")
        void excludesConversationsBeforeCutoff() {
            em.persistAndFlush(aConversation(alice, bob).topicKey("RIDE:1").build());
            em.clear();

            Instant future = Instant.now().plus(1, ChronoUnit.DAYS);
            List<Conversation> result = repository.findByParticipantIdAndUpdatedAtAfter(
                    alice.getId(), future, PageRequest.of(0, 10));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("List every conversation a user participates in")
    class FindAllByParticipantIdTests {

        @Test
        @DisplayName("Returns every conversation where the user is a participant")
        void returnsAllParticipantConversations() {
            em.persistAndFlush(aConversation(alice, bob).topicKey("RIDE:1").build());
            em.persistAndFlush(aConversation(alice, carol).topicKey("RIDE:2").build());
            em.persistAndFlush(aConversation(bob, carol).topicKey("RIDE:3").build());
            em.clear();

            List<Conversation> result = repository.findAllByParticipantId(alice.getId());

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("List conversations by topic key")
    class FindByTopicKeyTests {

        @Test
        @DisplayName("Returns only conversations that use the given topic key")
        void returnsConversationsWithMatchingTopicKey() {
            em.persistAndFlush(aConversation(alice, bob).topicKey("SEAT:7").build());
            em.persistAndFlush(aConversation(carol, alice).topicKey("RIDE:7").build());
            em.clear();

            List<Conversation> result = repository.findByTopicKey("SEAT:7");

            assertThat(result).singleElement()
                    .extracting(Conversation::getTopicKey).isEqualTo("SEAT:7");
        }
    }

    @Nested
    @DisplayName("Load conversation with participants eagerly fetched")
    class FindByIdWithParticipantsTests {

        @Test
        @DisplayName("Returns the conversation with both participant accounts initialised")
        void returnsConversationWithParticipantsLoaded() {
            Conversation conv = em.persistAndFlush(
                    aConversation(alice, bob).topicKey("RIDE:1").build());
            em.clear();

            Optional<Conversation> found = repository.findByIdWithParticipants(conv.getId());

            assertThat(found).isPresent()
                    .get().satisfies(c -> {
                        assertThat(c.getParticipantA().getEmail()).isEqualTo("alice@example.com");
                        assertThat(c.getParticipantB().getEmail()).isEqualTo("bob@example.com");
                    });
        }
    }

    @Nested
    @DisplayName("List conversations needing an email notification")
    class FindConversationsNeedingEmailTests {

        @Test
        @DisplayName("Returns conversations with unread messages that have not been emailed recently")
        void returnsConversationsWithUnreadAndStaleNotification() {
            Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
            Conversation needs = aConversation(alice, bob).topicKey("RIDE:1")
                    .lastMessageCreatedAt(past)
                    .participantBUnreadCount(3)
                    .build();
            em.persistAndFlush(needs);

            Conversation alreadyNotified = aConversation(alice, carol).topicKey("RIDE:2")
                    .lastMessageCreatedAt(past)
                    .participantBUnreadCount(1)
                    .participantBEmailNotifiedAt(Instant.now())
                    .build();
            em.persistAndFlush(alreadyNotified);
            em.clear();

            Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);
            List<Conversation> result = repository.findConversationsNeedingEmailNotification(cutoff);

            assertThat(result).singleElement()
                    .extracting(Conversation::getId).isEqualTo(needs.getId());
        }
    }

    @Nested
    @DisplayName("Delete every conversation for a participant")
    class DeleteAllByParticipantIdTests {

        @Test
        @DisplayName("Removes every conversation the user participates in and leaves others intact")
        void deletesAllConversationsForUser() {
            Conversation a = em.persistAndFlush(aConversation(alice, bob).topicKey("RIDE:1").build());
            Conversation b = em.persistAndFlush(aConversation(alice, carol).topicKey("RIDE:2").build());
            Conversation c = em.persistAndFlush(aConversation(bob, carol).topicKey("RIDE:3").build());
            em.clear();

            repository.deleteAllByParticipantId(alice.getId());
            em.flush();
            em.clear();

            assertThat(em.find(Conversation.class, a.getId())).isNull();
            assertThat(em.find(Conversation.class, b.getId())).isNull();
            assertThat(em.find(Conversation.class, c.getId())).isNotNull();
        }
    }
}

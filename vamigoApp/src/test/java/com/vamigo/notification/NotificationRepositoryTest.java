package com.vamigo.notification;

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
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static com.vamigo.util.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class NotificationRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private NotificationRepository repository;

    private UserAccount recipient;
    private UserAccount otherUser;

    @BeforeEach
    void setUp() {
        recipient = em.persistAndFlush(anActiveUserAccount().build());
        otherUser = em.persistAndFlush(anActiveUserAccount().email("other@example.com").build());
    }

    @Nested
    @DisplayName("Mark a single notification as read")
    class MarkReadByIdTests {

        @Test
        @DisplayName("Sets readAt and returns 1 when the recipient owns the notification")
        void returnsOneAndSetsReadAtForOwner() {
            Notification n = em.persistAndFlush(aNotification(recipient).build());
            em.clear();

            int updated = repository.markReadById(n.getId(), recipient.getId());
            em.clear();

            assertThat(updated).isEqualTo(1);
            assertThat(em.find(Notification.class, n.getId()).getReadAt()).isNotNull();
        }

        @Test
        @DisplayName("Returns 0 when the caller is not the recipient")
        void returnsZeroForWrongUser() {
            Notification n = em.persistAndFlush(aNotification(recipient).build());
            em.clear();

            int updated = repository.markReadById(n.getId(), otherUser.getId());

            assertThat(updated).isZero();
        }

        @Test
        @DisplayName("Returns 0 when the notification has already been read")
        void returnsZeroWhenAlreadyRead() {
            Notification n = em.persistAndFlush(
                    aNotification(recipient).readAt(Instant.now()).build());
            em.clear();

            int updated = repository.markReadById(n.getId(), recipient.getId());

            assertThat(updated).isZero();
        }
    }

    @Nested
    @DisplayName("Mark every unread notification for a user")
    class MarkAllReadTests {

        @Test
        @DisplayName("Marks all unread notifications for the user and leaves other users alone")
        void marksAllUnreadForUser() {
            em.persistAndFlush(aNotification(recipient).collapseKey("a").build());
            em.persistAndFlush(aNotification(recipient).collapseKey("b").build());
            em.persistAndFlush(aNotification(otherUser).collapseKey("c").build());
            em.clear();

            int updated = repository.markAllRead(recipient.getId());

            assertThat(updated).isEqualTo(2);
        }

        @Test
        @DisplayName("Returns 0 when every notification for the user is already read")
        void returnsZeroWhenNoUnread() {
            em.persistAndFlush(aNotification(recipient).readAt(Instant.now()).build());
            em.clear();

            int updated = repository.markAllRead(recipient.getId());

            assertThat(updated).isZero();
        }
    }

    @Nested
    @DisplayName("Purge read notifications older than a cutoff")
    class DeleteReadOlderThanTests {

        @Test
        @DisplayName("Deletes read notifications older than the cutoff and keeps newer/unread ones")
        void deletesReadNotificationsOlderThanCutoff() {
            Instant past = Instant.now().minus(2, ChronoUnit.DAYS);
            Notification old = em.persistAndFlush(
                    aNotification(recipient).readAt(past).collapseKey("old").build());
            Notification recent = em.persistAndFlush(
                    aNotification(recipient).readAt(Instant.now()).collapseKey("recent").build());
            Notification unread = em.persistAndFlush(
                    aNotification(recipient).collapseKey("unread").build());
            em.clear();

            Instant cutoff = Instant.now().minus(1, ChronoUnit.DAYS);
            int deleted = repository.deleteReadOlderThan(cutoff);

            assertThat(deleted).isEqualTo(1);
            assertThat(em.find(Notification.class, old.getId())).isNull();
            assertThat(em.find(Notification.class, recent.getId())).isNotNull();
            assertThat(em.find(Notification.class, unread.getId())).isNotNull();
        }

        @Test
        @DisplayName("Deletes nothing when no notifications are older than the cutoff")
        void deletesNoneWhenNothingOlder() {
            em.persistAndFlush(aNotification(recipient).readAt(Instant.now()).build());
            em.clear();

            int deleted = repository.deleteReadOlderThan(Instant.now().minus(1, ChronoUnit.HOURS));

            assertThat(deleted).isZero();
        }
    }

    @Nested
    @DisplayName("Page recipient's notifications excluding a given type")
    class FindByRecipientPagingTests {

        @Test
        @DisplayName("Returns only the recipient's notifications and excludes the filtered type")
        void filtersByRecipientAndExcludesType() {
            em.persistAndFlush(aNotification(recipient)
                    .notificationType(NotificationType.BOOKING_REQUESTED).collapseKey("a").build());
            em.persistAndFlush(aNotification(recipient)
                    .notificationType(NotificationType.CHAT_MESSAGE_NEW)
                    .channel(NotificationType.CHAT_MESSAGE_NEW.channel())
                    .collapseKey("b").build());
            em.persistAndFlush(aNotification(otherUser)
                    .notificationType(NotificationType.BOOKING_REQUESTED).collapseKey("c").build());
            em.clear();

            Slice<Notification> slice = repository
                    .findByRecipientIdAndNotificationTypeNotOrderByCreatedAtDesc(
                            recipient.getId(), NotificationType.CHAT_MESSAGE_NEW, PageRequest.of(0, 10));

            assertThat(slice.getContent()).hasSize(1)
                    .first().extracting(Notification::getNotificationType)
                    .isEqualTo(NotificationType.BOOKING_REQUESTED);
        }
    }

    @Nested
    @DisplayName("Find unread notification by collapse key")
    class FindByCollapseKeyTests {

        @Test
        @DisplayName("Returns the unread notification that matches the collapse key")
        void returnsOnlyUnreadMatchingCollapseKey() {
            em.persistAndFlush(aNotification(recipient).collapseKey("ride-1").build());
            em.persistAndFlush(aNotification(recipient).collapseKey("ride-1")
                    .readAt(Instant.now()).build());
            em.clear();

            Optional<Notification> found = repository
                    .findByRecipientIdAndCollapseKeyAndReadAtIsNull(recipient.getId(), "ride-1");

            assertThat(found).isPresent()
                    .get().extracting(Notification::getReadAt).isNull();
        }

        @Test
        @DisplayName("Returns empty when every notification with that collapse key is already read")
        void returnsEmptyWhenAllRead() {
            em.persistAndFlush(aNotification(recipient).collapseKey("ride-1")
                    .readAt(Instant.now()).build());
            em.clear();

            Optional<Notification> found = repository
                    .findByRecipientIdAndCollapseKeyAndReadAtIsNull(recipient.getId(), "ride-1");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Count unread notifications for a recipient")
    class CountUnreadTests {

        @Test
        @DisplayName("Counts only unread notifications that belong to the recipient")
        void countsOnlyUnreadForRecipient() {
            em.persistAndFlush(aNotification(recipient).collapseKey("a").build());
            em.persistAndFlush(aNotification(recipient).collapseKey("b").build());
            em.persistAndFlush(aNotification(recipient).collapseKey("c")
                    .readAt(Instant.now()).build());
            em.persistAndFlush(aNotification(otherUser).collapseKey("d").build());
            em.clear();

            long count = repository.countByRecipientIdAndReadAtIsNull(recipient.getId());

            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Count unread notifications excluding a given type")
    class CountUnreadExcludingTypeTests {

        @Test
        @DisplayName("Excludes the filtered notification type from the unread count")
        void excludesGivenType() {
            em.persistAndFlush(aNotification(recipient)
                    .notificationType(NotificationType.BOOKING_REQUESTED).collapseKey("a").build());
            em.persistAndFlush(aNotification(recipient)
                    .notificationType(NotificationType.CHAT_MESSAGE_NEW)
                    .channel(NotificationType.CHAT_MESSAGE_NEW.channel())
                    .collapseKey("b").build());
            em.clear();

            long count = repository.countByRecipientIdAndReadAtIsNullAndNotificationTypeNot(
                    recipient.getId(), NotificationType.CHAT_MESSAGE_NEW);

            assertThat(count).isEqualTo(1);
        }
    }
}

package com.blablatwo.notification;

import com.blablatwo.notification.dto.NotificationAlertDto;
import com.blablatwo.notification.dto.NotificationPageDto;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private PushNotificationService pushNotificationService;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @InjectMocks private NotificationService notificationService;

    private UserAccount recipient;

    @BeforeEach
    void setUp() {
        recipient = UserAccount.builder().build();
        lenient().when(userAccountRepository.findById(1L)).thenReturn(Optional.of(recipient));
    }

    @Nested
    @DisplayName("notify()")
    class Notify {

        @Test
        @DisplayName("should persist new notification and broadcast STOMP alert")
        void persistsAndBroadcasts() {
            var request = NotificationRequest.builder()
                    .recipientId(1L)
                    .type(NotificationType.BOOKING_REQUESTED)
                    .entityType(EntityType.RIDE)
                    .entityId("42")
                    .params(Map.of("offerKey", "r-42"))
                    .collapseKey("booking:99")
                    .build();

            when(notificationRepository.findByRecipientIdAndCollapseKeyAndReadAtIsNull(1L, "booking:99"))
                    .thenReturn(Optional.empty());
            when(notificationRepository.save(any(Notification.class)))
                    .thenAnswer(inv -> {
                        Notification n = inv.getArgument(0);
                        n.setId(UUID.randomUUID());
                        n.setCreatedAt(Instant.now());
                        return n;
                    });
            when(notificationRepository.countByRecipientIdAndReadAtIsNull(1L)).thenReturn(5L);

            notificationService.notify(request);

            verify(notificationRepository).save(any(Notification.class));

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(messagingTemplate).convertAndSendToUser(
                    eq("1"), eq("/queue/notifications"), payloadCaptor.capture());

            var alert = (NotificationAlertDto) payloadCaptor.getValue();
            assertEquals(NotificationType.BOOKING_REQUESTED, alert.type());
            assertEquals(5L, alert.unreadCount());
        }

        @Test
        @DisplayName("should collapse existing unread notification with same collapse key")
        void collapsesExistingNotification() {
            var existing = Notification.builder()
                    .recipient(recipient)
                    .notificationType(NotificationType.BOOKING_REQUESTED)
                    .channel(NotificationChannel.BOOKING_UPDATES)
                    .entityType(EntityType.RIDE)
                    .entityId("42")
                    .collapseKey("booking:99")
                    .count(2)
                    .build();
            existing.setId(UUID.randomUUID());
            existing.setCreatedAt(Instant.now().minusSeconds(60));

            var request = NotificationRequest.builder()
                    .recipientId(1L)
                    .type(NotificationType.BOOKING_REQUESTED)
                    .entityType(EntityType.RIDE)
                    .entityId("42")
                    .params(Map.of("offerKey", "r-42"))
                    .collapseKey("booking:99")
                    .build();

            when(notificationRepository.findByRecipientIdAndCollapseKeyAndReadAtIsNull(1L, "booking:99"))
                    .thenReturn(Optional.of(existing));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
            when(notificationRepository.countByRecipientIdAndReadAtIsNull(1L)).thenReturn(3L);

            notificationService.notify(request);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertEquals(3, captor.getValue().getCount());
        }

        @Test
        @DisplayName("should broadcast STOMP alert without DB persistence for CHAT_MESSAGE_NEW")
        void chatMessageBroadcastsWithoutPersistence() {
            var request = NotificationRequest.builder()
                    .recipientId(1L)
                    .type(NotificationType.CHAT_MESSAGE_NEW)
                    .entityType(EntityType.CONVERSATION)
                    .entityId("conv-uuid")
                    .params(Map.of("conversationId", "conv-uuid"))
                    .collapseKey("conv:conv-uuid")
                    .build();

            when(notificationRepository.countByRecipientIdAndReadAtIsNull(1L)).thenReturn(2L);

            notificationService.notify(request);

            verify(notificationRepository, never()).save(any());
            verify(pushNotificationService, never()).sendToUser(anyLong(), anyString(), anyString(), anyMap());

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(messagingTemplate).convertAndSendToUser(
                    eq("1"), eq("/queue/notifications"), payloadCaptor.capture());

            var alert = (NotificationAlertDto) payloadCaptor.getValue();
            assertEquals(NotificationType.CHAT_MESSAGE_NEW, alert.type());
            assertEquals(2L, alert.unreadCount());
        }

        @Test
        @DisplayName("should skip notification for unknown user")
        void skipsUnknownUser() {
            when(userAccountRepository.findById(999L)).thenReturn(Optional.empty());

            var request = NotificationRequest.builder()
                    .recipientId(999L)
                    .type(NotificationType.BOOKING_CONFIRMED)
                    .entityType(EntityType.RIDE)
                    .entityId("1")
                    .build();

            notificationService.notify(request);

            verify(notificationRepository, never()).save(any());
            verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("getNotifications()")
    class GetNotifications {

        @Test
        @DisplayName("should return paginated notifications with unread count")
        void returnsPaginatedNotifications() {
            var notification = Notification.builder()
                    .recipient(recipient)
                    .notificationType(NotificationType.BOOKING_CONFIRMED)
                    .channel(NotificationChannel.BOOKING_UPDATES)
                    .entityType(EntityType.RIDE)
                    .entityId("42")
                    .params(Map.of("offerKey", "r-42"))
                    .build();
            notification.setId(UUID.randomUUID());
            notification.setCreatedAt(Instant.now());

            var slice = new SliceImpl<>(List.of(notification), PageRequest.of(0, 20), false);
            when(notificationRepository.findByRecipientIdAndNotificationTypeNotOrderByCreatedAtDesc(
                    1L, NotificationType.CHAT_MESSAGE_NEW, PageRequest.of(0, 20)))
                    .thenReturn(slice);
            when(notificationRepository.countByRecipientIdAndReadAtIsNullAndNotificationTypeNot(
                    1L, NotificationType.CHAT_MESSAGE_NEW)).thenReturn(1L);

            NotificationPageDto result = notificationService.getNotifications(1L, 0, 20);

            assertEquals(1, result.notifications().size());
            assertFalse(result.hasMore());
            assertEquals(1L, result.unreadCount());
        }
    }

    @Nested
    @DisplayName("markRead()")
    class MarkRead {

        @Test
        @DisplayName("should mark read and broadcast count sync")
        void marksReadAndBroadcasts() {
            UUID id = UUID.randomUUID();
            when(notificationRepository.countByRecipientIdAndReadAtIsNullAndNotificationTypeNot(
                    1L, NotificationType.CHAT_MESSAGE_NEW)).thenReturn(2L);

            notificationService.markRead(1L, id);

            verify(notificationRepository).markReadById(id, 1L);
            verify(messagingTemplate).convertAndSendToUser(
                    eq("1"), eq("/queue/notification-sync"), any());
        }
    }
}

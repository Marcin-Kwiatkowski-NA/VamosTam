package com.vamigo.notification;

import com.vamigo.notification.dto.NotificationAlertDto;
import com.vamigo.notification.dto.NotificationPageDto;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tools.jackson.databind.json.JsonMapper;

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
    @Mock private PushMessageRenderer pushMessageRenderer;
    @Spy private JsonMapper jsonMapper = JsonMapper.builder().build();
    @InjectMocks private NotificationService notificationService;

    private UserAccount recipient;

    @BeforeEach
    void setUp() {
        recipient = UserAccount.builder().build();
        lenient().when(userAccountRepository.findById(1L)).thenReturn(Optional.of(recipient));
    }

    @Nested
    @DisplayName("Dispatch a notification to a recipient")
    class Notify {

        @Test
        @DisplayName("Persists the notification, broadcasts a STOMP alert, and sends a push with the deepLink payload")
        void persistsAndBroadcasts() {
            var params = Map.of("offerKey", "r-42", "deepLink", "/my-offer/r-42",
                    "origin", "Krakow", "destination", "Warsaw");
            var request = NotificationRequest.builder()
                    .recipientId(1L)
                    .type(NotificationType.BOOKING_REQUESTED)
                    .entityType(EntityType.RIDE)
                    .entityId("42")
                    .params(params)
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
            when(pushMessageRenderer.title(NotificationType.BOOKING_REQUESTED, params)).thenReturn("Krakow → Warsaw");
            when(pushMessageRenderer.body(NotificationType.BOOKING_REQUESTED, params)).thenReturn("Jan wants to join your ride");

            notificationService.notify(request);

            verify(notificationRepository).save(any(Notification.class));

            // Verify STOMP broadcast
            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(messagingTemplate).convertAndSendToUser(
                    eq("1"), eq("/queue/notifications"), payloadCaptor.capture());

            var alert = (NotificationAlertDto) payloadCaptor.getValue();
            assertEquals(NotificationType.BOOKING_REQUESTED, alert.type());
            assertEquals(5L, alert.unreadCount());

            // Verify push uses renderer and includes deepLink
            ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.captor();
            verify(pushNotificationService).sendToUser(eq(1L),
                    eq("Krakow → Warsaw"), eq("Jan wants to join your ride"), dataCaptor.capture());
            assertEquals("/my-offer/r-42", dataCaptor.getValue().get("deepLink"));
        }

        @Test
        @DisplayName("Collapses into the existing unread notification when the collapse key already exists")
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

            var params = Map.of("offerKey", "r-42");
            var request = NotificationRequest.builder()
                    .recipientId(1L)
                    .type(NotificationType.BOOKING_REQUESTED)
                    .entityType(EntityType.RIDE)
                    .entityId("42")
                    .params(params)
                    .collapseKey("booking:99")
                    .build();

            when(notificationRepository.findByRecipientIdAndCollapseKeyAndReadAtIsNull(1L, "booking:99"))
                    .thenReturn(Optional.of(existing));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
            when(notificationRepository.countByRecipientIdAndReadAtIsNull(1L)).thenReturn(3L);
            lenient().when(pushMessageRenderer.title(any(), any())).thenReturn("Booking update");
            lenient().when(pushMessageRenderer.body(any(), any())).thenReturn("Someone wants to join your ride");

            notificationService.notify(request);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertEquals(3, captor.getValue().getCount());
        }

        @Test
        @DisplayName("Broadcasts STOMP and push for CHAT_MESSAGE_NEW without writing anything to the database")
        void chatMessageBroadcastsWithoutPersistence() {
            var params = Map.of("conversationId", "conv-uuid", "senderName", "Anna",
                    "deepLink", "/chat/conv-uuid");
            var request = NotificationRequest.builder()
                    .recipientId(1L)
                    .type(NotificationType.CHAT_MESSAGE_NEW)
                    .entityType(EntityType.CONVERSATION)
                    .entityId("conv-uuid")
                    .params(params)
                    .collapseKey("conv:conv-uuid")
                    .build();

            when(notificationRepository.countByRecipientIdAndReadAtIsNull(1L)).thenReturn(2L);
            when(pushMessageRenderer.title(NotificationType.CHAT_MESSAGE_NEW, params)).thenReturn("Anna");
            when(pushMessageRenderer.body(NotificationType.CHAT_MESSAGE_NEW, params)).thenReturn("Message about Krakow → Warsaw");

            notificationService.notify(request);

            verify(notificationRepository, never()).save(any());

            // Verify push is dispatched with rendered title/body
            verify(pushNotificationService).sendToUser(eq(1L), eq("Anna"),
                    eq("Message about Krakow → Warsaw"), anyMap());

            // Verify deep link is included in FCM data
            ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.captor();
            verify(pushNotificationService).sendToUser(eq(1L), anyString(), anyString(), dataCaptor.capture());
            assertEquals("/chat/conv-uuid", dataCaptor.getValue().get("deepLink"));

            // Verify STOMP alert
            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(messagingTemplate).convertAndSendToUser(
                    eq("1"), eq("/queue/notifications"), payloadCaptor.capture());

            var alert = (NotificationAlertDto) payloadCaptor.getValue();
            assertEquals(NotificationType.CHAT_MESSAGE_NEW, alert.type());
            assertEquals(2L, alert.unreadCount());
        }

        @Test
        @DisplayName("Skips persistence and broadcast when the recipient user cannot be found")
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
    @DisplayName("Fetch a paginated notification feed")
    class GetNotifications {

        @Test
        @DisplayName("Returns a page of notifications together with the current unread count")
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
    @DisplayName("Mark a notification as read")
    class MarkRead {

        @Test
        @DisplayName("Marks the notification read and broadcasts the updated unread count via STOMP")
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

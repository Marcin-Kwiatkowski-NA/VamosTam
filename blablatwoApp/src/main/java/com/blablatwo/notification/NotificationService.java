package com.blablatwo.notification;

import com.blablatwo.notification.dto.NotificationAlertDto;
import com.blablatwo.notification.dto.NotificationPageDto;
import com.blablatwo.notification.dto.NotificationResponseDto;
import com.blablatwo.notification.dto.UnreadCountDto;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String NOTIFICATION_QUEUE = "/queue/notifications";
    private static final String NOTIFICATION_SYNC_QUEUE = "/queue/notification-sync";

    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userAccountRepository;
    private final PushNotificationService pushNotificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository,
                               UserAccountRepository userAccountRepository,
                               PushNotificationService pushNotificationService,
                               SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.userAccountRepository = userAccountRepository;
        this.pushNotificationService = pushNotificationService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Create or collapse a notification, broadcast via STOMP, and dispatch push.
     * Called from event listeners running @Async after commit.
     */
    @Transactional
    public void notify(NotificationRequest request) {
        try {
            UserAccount recipient = userAccountRepository.findById(request.recipientId()).orElse(null);
            if (recipient == null) {
                log.warn("Cannot notify unknown user {}", request.recipientId());
                return;
            }

            Notification notification = persistOrCollapse(recipient, request);

            long unreadCount = notificationRepository.countByRecipientIdAndReadAtIsNull(request.recipientId());

            broadcastStomp(request.recipientId(), notification, unreadCount);

            dispatchPush(request, notification);

        } catch (Exception e) {
            log.error("Failed to create notification for user {}: {}", request.recipientId(), e.getMessage(), e);
        }
    }

    // -- Notification Center queries --

    @Transactional(readOnly = true)
    public NotificationPageDto getNotifications(Long userId, int page, int size) {
        Slice<Notification> slice = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        long unreadCount = notificationRepository.countByRecipientIdAndReadAtIsNull(userId);
        return new NotificationPageDto(
                slice.getContent().stream().map(NotificationResponseDto::from).toList(),
                slice.hasNext(),
                unreadCount
        );
    }

    @Transactional(readOnly = true)
    public UnreadCountDto getUnreadCount(Long userId) {
        return new UnreadCountDto(notificationRepository.countByRecipientIdAndReadAtIsNull(userId));
    }

    @Transactional
    public void markRead(Long userId, UUID notificationId) {
        notificationRepository.markReadById(notificationId, userId);
        broadcastCountSync(userId);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId);
        broadcastCountSync(userId);
    }

    // -- Private helpers --

    private Notification persistOrCollapse(UserAccount recipient, NotificationRequest request) {
        if (request.collapseKey() != null) {
            var existing = notificationRepository
                    .findByRecipientIdAndCollapseKeyAndReadAtIsNull(request.recipientId(), request.collapseKey());
            if (existing.isPresent()) {
                Notification n = existing.get();
                n.collapse(request.params());
                return notificationRepository.save(n);
            }
        }

        Notification notification = Notification.builder()
                .recipient(recipient)
                .notificationType(request.type())
                .channel(request.channel())
                .entityType(request.entityType())
                .entityId(request.entityId())
                .params(request.params())
                .collapseKey(request.collapseKey())
                .build();
        return notificationRepository.save(notification);
    }

    private void broadcastStomp(Long recipientId, Notification notification, long unreadCount) {
        var alert = NotificationAlertDto.from(notification, unreadCount);
        messagingTemplate.convertAndSendToUser(
                recipientId.toString(), NOTIFICATION_QUEUE, alert);
    }

    private void broadcastCountSync(Long userId) {
        long unreadCount = notificationRepository.countByRecipientIdAndReadAtIsNull(userId);
        messagingTemplate.convertAndSendToUser(
                userId.toString(), NOTIFICATION_SYNC_QUEUE,
                Map.of("unreadCount", unreadCount));
    }

    private void dispatchPush(NotificationRequest request, Notification notification) {
        pushNotificationService.sendToUser(
                request.recipientId(),
                request.type().name(),
                request.type().name(),
                Map.of(
                        "type", request.type().name(),
                        "entityType", request.entityType().name(),
                        "entityId", request.entityId(),
                        "collapseKey", request.collapseKey() != null ? request.collapseKey() : "",
                        "channel", request.channel().name(),
                        "notificationId", notification.getId().toString()
                )
        );
    }
}

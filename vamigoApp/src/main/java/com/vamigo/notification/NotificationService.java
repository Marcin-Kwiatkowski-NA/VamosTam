package com.vamigo.notification;

import com.vamigo.notification.dto.NotificationAlertDto;
import com.vamigo.notification.dto.NotificationPageDto;
import com.vamigo.notification.dto.NotificationResponseDto;
import com.vamigo.notification.dto.UnreadCountDto;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String NOTIFICATION_QUEUE = "/queue/notifications";
    private static final String NOTIFICATION_SYNC_QUEUE = "/queue/notification-sync";
    private static final String ROUTE_VERSION = "1";
    /** FCM data map is rejected over 4 KB. Warn before we approach the cliff. */
    private static final int PAYLOAD_WARN_THRESHOLD = 3500;

    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userAccountRepository;
    private final PushNotificationService pushNotificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final PushMessageRenderer pushMessageRenderer;
    private final JsonMapper jsonMapper;

    public NotificationService(NotificationRepository notificationRepository,
                               UserAccountRepository userAccountRepository,
                               PushNotificationService pushNotificationService,
                               SimpMessagingTemplate messagingTemplate,
                               PushMessageRenderer pushMessageRenderer,
                               JsonMapper jsonMapper) {
        this.notificationRepository = notificationRepository;
        this.userAccountRepository = userAccountRepository;
        this.pushNotificationService = pushNotificationService;
        this.messagingTemplate = messagingTemplate;
        this.pushMessageRenderer = pushMessageRenderer;
        this.jsonMapper = jsonMapper;
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

            // Chat messages: STOMP alert (no DB persistence) + FCM push
            if (request.type() == NotificationType.CHAT_MESSAGE_NEW) {
                long unreadCount = notificationRepository.countByRecipientIdAndReadAtIsNull(request.recipientId());
                var alert = NotificationAlertDto.fromRequest(request, unreadCount);
                messagingTemplate.convertAndSendToUser(
                        request.recipientId().toString(), NOTIFICATION_QUEUE, alert);

                var chatData = buildFcmData(request, null);
                pushNotificationService.sendToUser(
                        request.recipientId(),
                        pushMessageRenderer.title(request.type(), request.params()),
                        pushMessageRenderer.body(request.type(), request.params()),
                        chatData
                );
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
                .findByRecipientIdAndNotificationTypeNotOrderByCreatedAtDesc(
                        userId, NotificationType.CHAT_MESSAGE_NEW, PageRequest.of(page, size));
        long unreadCount = notificationRepository
                .countByRecipientIdAndReadAtIsNullAndNotificationTypeNot(userId, NotificationType.CHAT_MESSAGE_NEW);
        return new NotificationPageDto(
                slice.getContent().stream().map(NotificationResponseDto::from).toList(),
                slice.hasNext(),
                unreadCount
        );
    }

    @Transactional(readOnly = true)
    public UnreadCountDto getUnreadCount(Long userId) {
        return new UnreadCountDto(notificationRepository
                .countByRecipientIdAndReadAtIsNullAndNotificationTypeNot(userId, NotificationType.CHAT_MESSAGE_NEW));
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

    /**
     * Mark all of the user's unread notifications targeting a given entity as read.
     * Idempotent — safe to call regardless of whether any rows match. Broadcasts
     * a count-sync only when at least one row is updated.
     */
    @Transactional
    public int markReadByEntity(Long userId, EntityType entityType, String entityId) {
        int affectedRows = notificationRepository.markReadByEntity(userId, entityType, entityId);
        log.info("markReadByEntity user={} entityType={} entityId={} affectedRows={}",
                userId, entityType, entityId, affectedRows);
        if (affectedRows > 0) {
            broadcastCountSync(userId);
        }
        return affectedRows;
    }

    // -- Private helpers --

    private Notification persistOrCollapse(UserAccount recipient, NotificationRequest request) {
        Map<String, String> augmentedParams = paramsWithTypedFields(request);
        if (request.collapseKey() != null) {
            var existing = notificationRepository
                    .findByRecipientIdAndCollapseKeyAndReadAtIsNull(request.recipientId(), request.collapseKey());
            if (existing.isPresent()) {
                Notification n = existing.get();
                n.collapse(augmentedParams);
                return notificationRepository.save(n);
            }
        }

        Notification notification = Notification.builder()
                .recipient(recipient)
                .notificationType(request.type())
                .channel(request.channel())
                .entityType(request.entityType())
                .entityId(request.entityId())
                .params(augmentedParams)
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
        long unreadCount = notificationRepository
                .countByRecipientIdAndReadAtIsNullAndNotificationTypeNot(userId, NotificationType.CHAT_MESSAGE_NEW);
        messagingTemplate.convertAndSendToUser(
                userId.toString(), NOTIFICATION_SYNC_QUEUE,
                Map.of("unreadCount", unreadCount));
    }

    private void dispatchPush(NotificationRequest request, Notification notification) {
        var data = buildFcmData(request, notification.getId().toString());
        data.put("channel", request.channel().name());
        data.put("notificationId", notification.getId().toString());

        // R2: render bigBody server-side and forward via FCM data so foreground
        // handlers (Android local-notification BigText, iOS NSE) can show the
        // richer expansion. Falls back to short body when the renderer returns
        // null for this type.
        String bigBody = pushMessageRenderer.bigBody(request.type(), request.params());
        if (bigBody != null && !data.containsKey("bigBody")) {
            data.put("bigBody", bigBody);
        }

        pushNotificationService.sendToUser(
                request.recipientId(),
                pushMessageRenderer.title(request.type(), request.params()),
                pushMessageRenderer.body(request.type(), request.params()),
                data
        );
    }

    Map<String, String> buildFcmData(NotificationRequest request, String notificationId) {
        var data = new LinkedHashMap<String, String>();
        data.put("routeVersion", ROUTE_VERSION);
        data.put("type", request.type().name());
        data.put("entityType", request.entityType().name());
        data.put("entityId", request.entityId() != null ? request.entityId() : "");
        data.putAll(typedFieldsForParams(request));
        data.put("collapseKey", request.collapseKey() != null ? request.collapseKey() : "");
        // Compat: derived deep link for pre-v1 clients. Client must prefer targetType.
        if (request.params() != null && request.params().containsKey("deepLink")) {
            data.put("deepLink", request.params().get("deepLink"));
        }
        // R2 rich fields — forwarded verbatim if present.
        if (request.params() != null && request.params().containsKey("bigBody")) {
            data.put("bigBody", request.params().get("bigBody"));
        }

        int size = estimatePayloadSize(data);
        if (size > PAYLOAD_WARN_THRESHOLD) {
            log.warn("FCM data payload approaching 4 KB limit (size={} type={} targetType={})",
                    size, request.type(), request.effectiveTargetType());
        } else {
            log.debug("FCM data payload size={} type={}", size, request.type());
        }

        return data;
    }

    /**
     * Typed v1 route fields ({@code targetType}, {@code resultKind},
     * {@code listFilters}) as stringified map entries. Used by both FCM
     * {@code data} and persisted {@code Notification.params} so REST-fetched
     * rows resolve routes identically to warm/cold FCM taps.
     */
    private Map<String, String> typedFieldsForParams(NotificationRequest request) {
        var typed = new LinkedHashMap<String, String>();
        typed.put("targetType", request.effectiveTargetType().name());
        if (request.resultKind() != null) {
            typed.put("resultKind", request.resultKind().name());
        }
        if (request.listFilters() != null && !request.listFilters().isEmpty()) {
            try {
                typed.put("listFilters", jsonMapper.writeValueAsString(request.listFilters()));
            } catch (JacksonException e) {
                log.warn("Failed to serialize listFilters for notification (type={}): {}",
                        request.type(), e.getMessage());
            }
        }
        return typed;
    }

    /**
     * Merge request params with the typed v1 route fields. Typed fields win
     * on key collision so the client resolver sees a canonical payload.
     */
    private Map<String, String> paramsWithTypedFields(NotificationRequest request) {
        var merged = new LinkedHashMap<String, String>();
        if (request.params() != null) {
            merged.putAll(request.params());
        }
        merged.putAll(typedFieldsForParams(request));
        return merged;
    }

    private static int estimatePayloadSize(Map<String, String> data) {
        int size = 0;
        for (var e : data.entrySet()) {
            if (e.getKey() != null) size += e.getKey().length();
            if (e.getValue() != null) size += e.getValue().length();
        }
        return size;
    }
}

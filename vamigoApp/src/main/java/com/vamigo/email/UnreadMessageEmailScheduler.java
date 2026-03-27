package com.vamigo.email;

import com.vamigo.domain.PersonDisplayNameResolver;
import com.vamigo.messaging.Conversation;
import com.vamigo.messaging.ConversationRepository;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserProfile;
import com.vamigo.user.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class UnreadMessageEmailScheduler {

    private static final Logger log = LoggerFactory.getLogger(UnreadMessageEmailScheduler.class);
    private static final int MAX_PREVIEW_LENGTH = 100;

    private final ConversationRepository conversationRepository;
    private final UserProfileRepository userProfileRepository;
    private final PersonDisplayNameResolver displayNameResolver;
    private final BrevoClient brevoClient;
    private final long unreadMessageTemplateId;
    private final long unreadMessageTemplateIdPl;
    private final int delayMinutes;

    public UnreadMessageEmailScheduler(ConversationRepository conversationRepository,
                                       UserProfileRepository userProfileRepository,
                                       PersonDisplayNameResolver displayNameResolver,
                                       BrevoClient brevoClient,
                                       @Value("${brevo.unread-message-template-id}") long unreadMessageTemplateId,
                                       @Value("${brevo.unread-message-template-id-pl}") long unreadMessageTemplateIdPl,
                                       @Value("${message-email.delay-minutes}") int delayMinutes) {
        this.conversationRepository = conversationRepository;
        this.userProfileRepository = userProfileRepository;
        this.displayNameResolver = displayNameResolver;
        this.brevoClient = brevoClient;
        this.unreadMessageTemplateId = unreadMessageTemplateId;
        this.unreadMessageTemplateIdPl = unreadMessageTemplateIdPl;
        this.delayMinutes = delayMinutes;
    }

    @Scheduled(fixedDelayString = "${message-email.check-interval-ms}")
    @Transactional
    public void checkUnreadMessages() {
        Instant cutoff = Instant.now().minus(delayMinutes, ChronoUnit.MINUTES);
        List<Conversation> conversations = conversationRepository.findConversationsNeedingEmailNotification(cutoff);

        if (conversations.isEmpty()) return;

        log.info("Found {} conversations needing unread email notification", conversations.size());
        Instant now = Instant.now();

        for (Conversation conversation : conversations) {
            notifyParticipantIfNeeded(conversation, conversation.getParticipantA(),
                    conversation.getParticipantAUnreadCount(),
                    conversation.getParticipantAEmailNotifiedAt(),
                    true, now);

            notifyParticipantIfNeeded(conversation, conversation.getParticipantB(),
                    conversation.getParticipantBUnreadCount(),
                    conversation.getParticipantBEmailNotifiedAt(),
                    false, now);
        }
    }

    private void notifyParticipantIfNeeded(Conversation conversation, UserAccount recipient,
                                           int unreadCount, Instant emailNotifiedAt,
                                           boolean isParticipantA, Instant now) {
        if (unreadCount <= 0) return;
        if (emailNotifiedAt != null && !emailNotifiedAt.isBefore(conversation.getLastMessageCreatedAt())) return;

        try {
            Long senderId = conversation.getLastMessageSenderId();
            String senderName = resolveDisplayName(senderId);
            String recipientName = resolveDisplayName(recipient.getId());
            String messagePreview = truncatePreview(conversation.getLastMessageBody());

            Map<String, String> params = new LinkedHashMap<>();
            params.put("SENDER_NAME", senderName);
            params.put("UNREAD_COUNT", String.valueOf(unreadCount));
            params.put("MESSAGE_PREVIEW", messagePreview);
            params.put("DEEP_LINK", "/chat/" + conversation.getId());

            brevoClient.sendTemplateEmail(
                    recipient.getEmail(),
                    recipientName,
                    unreadMessageTemplateIdPl,
                    params);

            if (isParticipantA) {
                conversation.setParticipantAEmailNotifiedAt(now);
            } else {
                conversation.setParticipantBEmailNotifiedAt(now);
            }

            log.info("Unread message email sent to user {} for conversation {}", recipient.getId(), conversation.getId());
        } catch (Exception e) {
            log.error("Failed to send unread message email to user {} for conversation {}: {}",
                    recipient.getId(), conversation.getId(), e.getMessage());
        }
    }

    private String resolveDisplayName(Long userId) {
        UserProfile profile = userProfileRepository.findById(userId).orElse(null);
        return displayNameResolver.resolveInternal(profile, userId);
    }

    private static String truncatePreview(String body) {
        if (body == null) return "";
        return body.length() > MAX_PREVIEW_LENGTH
                ? body.substring(0, MAX_PREVIEW_LENGTH - 3) + "..."
                : body;
    }
}

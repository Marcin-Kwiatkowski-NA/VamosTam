package com.vamigo.messaging;

import com.vamigo.auth.AppPrincipal;
import com.vamigo.config.security.AppJwtAuthenticationToken;
import com.vamigo.messaging.dto.AckDeliveredRequest;
import com.vamigo.messaging.dto.MarkReadRequest;
import com.vamigo.messaging.dto.SendMessageRequest;
import com.vamigo.messaging.dto.StompErrorDto;
import com.vamigo.messaging.exception.ConversationNotFoundException;
import com.vamigo.messaging.exception.NotParticipantException;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
public class StompMessagingController {

    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;

    public StompMessagingController(ConversationService conversationService,
                                    SimpMessagingTemplate messagingTemplate) {
        this.conversationService = conversationService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/conversation/{conversationId}/send")
    public void sendMessage(
            @DestinationVariable UUID conversationId,
            @Valid @Payload SendMessageRequest request,
            Principal principal) {

        AppPrincipal appPrincipal = ((AppJwtAuthenticationToken) principal).getPrincipal();
        conversationService.sendMessage(conversationId, request, appPrincipal.userId());
    }

    @MessageMapping("/conversation/{conversationId}/ack-delivered")
    public void ackDelivered(
            @DestinationVariable UUID conversationId,
            @Payload AckDeliveredRequest request,
            Principal principal) {

        AppPrincipal appPrincipal = ((AppJwtAuthenticationToken) principal).getPrincipal();
        conversationService.markDelivered(conversationId, request.lastMessageId(), appPrincipal.userId());
    }

    @MessageMapping("/conversation/{conversationId}/mark-read")
    public void markRead(
            @DestinationVariable UUID conversationId,
            @Payload MarkReadRequest request,
            Principal principal) {

        AppPrincipal appPrincipal = ((AppJwtAuthenticationToken) principal).getPrincipal();
        conversationService.markRead(conversationId, request.lastMessageId(), appPrincipal.userId());
    }

    @MessageExceptionHandler(ConversationNotFoundException.class)
    public void handleConversationNotFound(ConversationNotFoundException ex, Principal principal) {
        sendError(principal, "CONVERSATION_NOT_FOUND", ex.getMessage(), null);
    }

    @MessageExceptionHandler(NotParticipantException.class)
    public void handleNotParticipant(NotParticipantException ex, Principal principal) {
        sendError(principal, "NOT_PARTICIPANT", ex.getMessage(), null);
    }

    @MessageExceptionHandler(Exception.class)
    public void handleGenericError(Exception ex, Principal principal) {
        sendError(principal, "SEND_FAILED", "Failed to send message", null);
    }

    private void sendError(Principal principal, String code, String message, UUID conversationId) {
        if (principal == null) return;
        String userId = principal.getName();
        messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/errors",
                new StompErrorDto(code, message, conversationId)
        );
    }
}

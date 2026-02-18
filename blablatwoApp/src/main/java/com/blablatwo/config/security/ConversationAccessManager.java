package com.blablatwo.config.security;

import com.blablatwo.auth.AppPrincipal;
import com.blablatwo.messaging.ConversationRepository;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.messaging.access.intercept.MessageAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class ConversationAccessManager implements AuthorizationManager<MessageAuthorizationContext<?>> {

    private final ConversationRepository conversationRepository;

    public ConversationAccessManager(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    @Override
    public AuthorizationResult authorize(Supplier<? extends Authentication> authentication,
                                          MessageAuthorizationContext<?> context) {
        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }

        Map<String, String> variables = context.getVariables();
        String conversationIdStr = variables.get("conversationId");
        if (conversationIdStr == null) {
            return new AuthorizationDecision(false);
        }

        UUID conversationId;
        try {
            conversationId = UUID.fromString(conversationIdStr);
        } catch (IllegalArgumentException e) {
            return new AuthorizationDecision(false);
        }

        AppPrincipal principal = ((AppJwtAuthenticationToken) auth).getPrincipal();
        Long userId = principal.userId();

        boolean isParticipant = conversationRepository.findById(conversationId)
                .map(conv -> conv.getParticipantA().getId().equals(userId)
                        || conv.getParticipantB().getId().equals(userId))
                .orElse(false);

        return new AuthorizationDecision(isParticipant);
    }
}

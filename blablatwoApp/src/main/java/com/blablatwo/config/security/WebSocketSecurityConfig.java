package com.blablatwo.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

    private final ConversationAccessManager conversationAccessManager;

    public WebSocketSecurityConfig(ConversationAccessManager conversationAccessManager) {
        this.conversationAccessManager = conversationAccessManager;
    }

    /**
     * Disable CSRF for WebSocket STOMP connections.
     * JWT authentication via {@link JwtStompInterceptor} makes CSRF redundant —
     * there is no session cookie to protect.
     */
    @Bean("csrfChannelInterceptor")
    ChannelInterceptor csrfChannelInterceptor() {
        return new ChannelInterceptor() {};
    }

    @Bean
    AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages) {
        return messages
                .nullDestMatcher().authenticated()
                .simpSubscribeDestMatchers("/user/queue/**").authenticated()
                .simpSubscribeDestMatchers("/topic/conversation/{conversationId}").access(conversationAccessManager)
                .simpDestMatchers("/app/**").authenticated()
                .anyMessage().denyAll()
                .build();
    }
}
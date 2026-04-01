package com.vamigo.notification;

import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * Renders contextual push notification title/body using i18n message templates.
 * Uses enriched params (route labels, counterparty names) when available,
 * falls back to generic messages when params are absent.
 */
@Component
public class PushMessageRenderer {

    private final MessageSource messageSource;

    public PushMessageRenderer() {
        var source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:messages/notifications");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        this.messageSource = source;
    }

    public String title(NotificationType type, Map<String, String> params) {
        return title(type, params, Locale.ENGLISH);
    }

    public String title(NotificationType type, Map<String, String> params, Locale locale) {
        String prefix = "push." + type.name().toLowerCase();
        String routeLabel = routeLabel(params);

        return switch (type) {
            case CHAT_MESSAGE_NEW -> {
                String senderName = param(params, "senderName");
                yield senderName != null
                        ? resolve(prefix + ".title", locale, senderName)
                        : resolve(prefix + ".title.fallback", locale);
            }
            case REVIEW_RECEIVED -> resolve(prefix + ".title", locale);
            default -> routeLabel != null
                    ? resolve(prefix + ".title", locale, routeLabel)
                    : resolve(prefix + ".title.fallback", locale);
        };
    }

    public String body(NotificationType type, Map<String, String> params) {
        return body(type, params, Locale.ENGLISH);
    }

    public String body(NotificationType type, Map<String, String> params, Locale locale) {
        String prefix = "push." + type.name().toLowerCase();
        String routeLabel = routeLabel(params);

        return switch (type) {
            case CHAT_MESSAGE_NEW -> routeLabel != null
                    ? resolve(prefix + ".body", locale, routeLabel)
                    : resolve(prefix + ".body.fallback", locale);
            case BOOKING_REQUESTED -> {
                String name = param(params, "counterpartyName");
                yield name != null
                        ? resolve(prefix + ".body", locale, name)
                        : resolve(prefix + ".body.fallback", locale);
            }
            case BOOKING_CANCELLED -> {
                String name = param(params, "counterpartyName");
                String reason = param(params, "reason");
                String base = name != null
                        ? resolve(prefix + ".body", locale, name)
                        : resolve(prefix + ".body.fallback", locale);
                yield reason != null ? base + ": " + reason : base;
            }
            case SEARCH_ALERT_MATCH -> {
                String count = param(params, "matchCount");
                yield count != null
                        ? resolve(prefix + ".body", locale, count)
                        : resolve(prefix + ".body.fallback", locale);
            }
            case REVIEW_RECEIVED -> resolve(prefix + ".body", locale);
            default -> resolve(prefix + ".body", locale);
        };
    }

    private String resolve(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args.length > 0 ? args : null, key, locale);
    }

    private static String param(Map<String, String> params, String key) {
        if (params == null) return null;
        String val = params.get(key);
        return val != null && !val.isBlank() ? val : null;
    }

    private static String routeLabel(Map<String, String> params) {
        return NotificationParamsEnricher.routeLabel(
                param(params, "origin"), param(params, "destination"));
    }
}

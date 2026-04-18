package com.vamigo.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Renders contextual push notification title/body using i18n message templates.
 * Uses enriched params (route labels, counterparty names) when available,
 * falls back to generic messages when params are absent.
 */
@Component
public class PushMessageRenderer {

    private static final Logger log = LoggerFactory.getLogger(PushMessageRenderer.class);

    private final MessageSource messageSource;
    private final JsonMapper jsonMapper;

    public PushMessageRenderer(JsonMapper jsonMapper) {
        var source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:messages/notifications");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        this.messageSource = source;
        this.jsonMapper = jsonMapper;
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
            case SEARCH_ALERT_MATCH -> {
                String dateFmt = param(params, "departureDateFmt");
                if (routeLabel != null && dateFmt != null) {
                    yield resolve(prefix + ".title.with_date", locale, routeLabel, dateFmt);
                }
                yield routeLabel != null
                        ? resolve(prefix + ".title", locale, routeLabel)
                        : resolve(prefix + ".title.fallback", locale);
            }
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
                String earliest = param(params, "earliestDeparture");
                String minPrice = param(params, "minPrice");
                String driverName = param(params, "driverName");
                int matchCount = parseIntOr(count, 0);

                if (matchCount == 1 && earliest != null) {
                    if (driverName != null && minPrice != null) {
                        yield resolve(prefix + ".body.single", locale, earliest, driverName, minPrice);
                    }
                    if (minPrice != null) {
                        yield resolve(prefix + ".body.single.no_name", locale, earliest, minPrice);
                    }
                }
                if (matchCount > 1 && earliest != null && minPrice != null) {
                    yield resolve(prefix + ".body.multi", locale, count, minPrice, earliest);
                }
                yield count != null
                        ? resolve(prefix + ".body", locale, count)
                        : resolve(prefix + ".body.fallback", locale);
            }
            case REVIEW_RECEIVED -> resolve(prefix + ".body", locale);
            default -> resolve(prefix + ".body", locale);
        };
    }

    /**
     * Multi-line preview body for Android BigText style on SearchAlert pushes.
     * Returns null when no preview rows are encoded — caller should then fall
     * back to the regular body so we don't show an empty expansion.
     */
    public String bigBody(NotificationType type, Map<String, String> params) {
        return bigBody(type, params, Locale.ENGLISH);
    }

    public String bigBody(NotificationType type, Map<String, String> params, Locale locale) {
        if (type != NotificationType.SEARCH_ALERT_MATCH) return null;
        String previewJson = param(params, "previewRows");
        if (previewJson == null) return null;

        try {
            List<Map<String, Object>> rows = jsonMapper.readValue(
                    previewJson, new tools.jackson.core.type.TypeReference<>() {});
            if (rows.isEmpty()) return null;

            String rowFormat = resolve("push.search_alert_match.preview.row", locale,
                    "{0}", "{1}", "{2}");
            var sb = new StringBuilder();
            for (var row : rows) {
                String time = stringOrEmpty(row.get("time"));
                String name = stringOrEmpty(row.get("name"));
                String price = stringOrEmpty(row.get("price"));
                String formatted = java.text.MessageFormat.format(
                        rowFormat, time, name, price);
                if (sb.length() > 0) sb.append('\n');
                sb.append(formatted);
            }
            return NotificationParamsEnricher.capBigBody(sb.toString());
        } catch (JacksonException e) {
            log.warn("Failed to decode SearchAlert previewRows: {}", e.getMessage());
            return null;
        }
    }

    private String resolve(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args.length > 0 ? args : null, key, locale);
    }

    private static String stringOrEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    private static int parseIntOr(String value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
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

package com.vamigo.user;

import java.text.Normalizer;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class SlugUtils {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{1,98}[a-z0-9]$");
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9-]");
    private static final Pattern CONSECUTIVE_HYPHENS = Pattern.compile("-{2,}");

    private static final Set<String> RESERVED_SLUGS = Set.of(
            "rides", "ride", "seats", "seat", "user", "auth", "me", "login",
            "register", "profile", "messages", "chat", "post-ride", "post-seat",
            "my-offers", "my-offer", "packages", "notifications", "reviews",
            "admin", "api", "ws", "error", "carrier-profile", "carriers",
            "search", "p", "offer", "map", "dev", "hello", "h2-console",
            "privacy-policy", "terms", "forgot-password", "reset-password",
            "change-password", "search-alerts", "create-account",
            "create-carrier-account", "splash", "public", "verify-result",
            "booking-result", "edit-ride", "edit-seat", "vehicles"
    );

    private SlugUtils() {
    }

    public static String generateSlug(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        // 1. Manual replacement for characters NFD doesn't decompose
        String result = name
                .replace('ł', 'l')
                .replace('Ł', 'L');

        // 2. NFD normalization + strip combining marks (ó→o, ź→z, etc.)
        result = Normalizer.normalize(result, Normalizer.Form.NFD);
        result = COMBINING_MARKS.matcher(result).replaceAll("");

        // 3. Lowercase, spaces/underscores → hyphens
        result = result.toLowerCase();
        result = result.replace(' ', '-').replace('_', '-');

        // 4. Strip non-alphanumeric/non-hyphen
        result = NON_SLUG_CHARS.matcher(result).replaceAll("");

        // 5. Collapse consecutive hyphens, trim leading/trailing hyphens
        result = CONSECUTIVE_HYPHENS.matcher(result).replaceAll("-");
        result = result.replaceAll("^-+|-+$", "");

        // 6. Enforce length 3–100
        if (result.length() > 100) {
            result = result.substring(0, 100).replaceAll("-+$", "");
        }

        return result;
    }

    public static boolean isValid(String slug) {
        return slug != null && SLUG_PATTERN.matcher(slug).matches();
    }

    public static boolean isReserved(String slug) {
        return slug != null && RESERVED_SLUGS.contains(slug.toLowerCase());
    }

    public static String makeUnique(String baseSlug, Predicate<String> existsCheck) {
        if (!existsCheck.test(baseSlug)) {
            return baseSlug;
        }
        for (int i = 2; i <= 1000; i++) {
            String candidate = baseSlug + "-" + i;
            if (candidate.length() > 100) {
                // Truncate base to make room for suffix
                int maxBase = 100 - String.valueOf(i).length() - 1;
                candidate = baseSlug.substring(0, maxBase).replaceAll("-+$", "") + "-" + i;
            }
            if (!existsCheck.test(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate unique slug for: " + baseSlug);
    }
}

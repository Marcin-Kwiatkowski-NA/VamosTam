package com.vamigo.auth.service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.vamigo.user.Role;
import com.vamigo.user.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final long ACCESS_TTL_MS = 15 * 60 * 1000L;
    private static final long REFRESH_TTL_MS = 7L * 24 * 60 * 60 * 1000L;

    private MutableClock clock;
    private JwtDecoder decoder;
    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        SecretKey key = new SecretKeySpec(secret, "HmacSHA256");

        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        NimbusJwtDecoder nimbusDecoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        clock = new MutableClock(FIXED_NOW);
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator();
        timestampValidator.setClock(clock);
        nimbusDecoder.setJwtValidator(timestampValidator);
        this.decoder = nimbusDecoder;

        provider = new JwtTokenProvider(encoder, decoder, clock, ACCESS_TTL_MS, REFRESH_TTL_MS);
    }

    @Test
    @DisplayName("generateToken stamps iat and exp from injected clock")
    void generateToken_usesClock() {
        UserAccount user = userBuilder().build();

        Jwt jwt = decoder.decode(provider.generateToken(user));

        assertThat(jwt.getIssuedAt()).isEqualTo(FIXED_NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(FIXED_NOW.plusMillis(ACCESS_TTL_MS));
        assertThat(((Number) jwt.getClaim("userId")).longValue()).isEqualTo(42L);
        assertThat(jwt.getClaimAsString("email")).isEqualTo("test@example.com");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactlyInAnyOrder("USER");
        assertThat(jwt.getClaimAsString("type")).isEqualTo("access");
        assertThat(((Number) jwt.getClaim("tv")).intValue()).isEqualTo(3);
    }

    @Test
    @DisplayName("generateRefreshToken stamps refresh ttl and type=refresh")
    void generateRefreshToken_usesRefreshTtl() {
        UserAccount user = userBuilder().build();

        String token = provider.generateRefreshToken(user);
        Jwt jwt = decoder.decode(token);

        assertThat(jwt.getIssuedAt()).isEqualTo(FIXED_NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(FIXED_NOW.plusMillis(REFRESH_TTL_MS));
        assertThat(jwt.getClaimAsString("type")).isEqualTo("refresh");
        assertThat(provider.isRefreshToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken returns false once clock advances past exp + skew")
    void validateToken_falseAfterExpiry() {
        String token = provider.generateToken(userBuilder().build());
        assertThat(provider.validateToken(token)).isTrue();

        // Default JwtTimestampValidator skew is 60s; advance well past it.
        clock.advanceTo(FIXED_NOW.plusMillis(ACCESS_TTL_MS).plusSeconds(120));

        assertThat(provider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("isRefreshToken distinguishes access tokens from refresh tokens")
    void isRefreshToken_distinguishesTypes() {
        UserAccount user = userBuilder().build();

        assertThat(provider.isRefreshToken(provider.generateToken(user))).isFalse();
        assertThat(provider.isRefreshToken(provider.generateRefreshToken(user))).isTrue();
    }

    @Test
    @DisplayName("validateToken returns false for an unsigned/garbage token")
    void validateToken_falseForGarbage() {
        assertThat(provider.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    @DisplayName("getUserIdFromToken and getTokenVersionFromToken read numeric claims")
    void readsNumericClaims() {
        String token = provider.generateToken(userBuilder().tokenVersion(7).build());

        assertThat(provider.getUserIdFromToken(token)).isEqualTo(42L);
        assertThat(provider.getTokenVersionFromToken(token)).isEqualTo(7);
        assertThat(provider.getEmailFromToken(token)).isEqualTo("test@example.com");
        assertThat(provider.getRolesFromToken(token)).containsExactlyInAnyOrder("USER");
    }

    private UserAccount.UserAccountBuilder userBuilder() {
        return UserAccount.builder()
                .id(42L)
                .email("test@example.com")
                .roles(Set.of(Role.USER))
                .tokenVersion(3);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advanceTo(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}

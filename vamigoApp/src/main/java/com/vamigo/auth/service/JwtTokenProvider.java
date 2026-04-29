package com.vamigo.auth.service;

import com.vamigo.user.Role;
import com.vamigo.user.UserAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class JwtTokenProvider {

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String TOKEN_VERSION_CLAIM = "tv";

    private static final JwsHeader HS256_HEADER = JwsHeader.with(MacAlgorithm.HS256).build();

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Clock clock;
    private final long jwtExpirationMs;
    private final long jwtRefreshExpirationMs;

    public JwtTokenProvider(
            JwtEncoder jwtEncoder,
            JwtDecoder jwtDecoder,
            Clock clock,
            @Value("${app.jwt.expiration-ms}") long jwtExpirationMs,
            @Value("${app.jwt.refresh-expiration-ms:604800000}") long jwtRefreshExpirationMs) {
        this.encoder = jwtEncoder;
        this.decoder = jwtDecoder;
        this.clock = clock;
        this.jwtExpirationMs = jwtExpirationMs;
        this.jwtRefreshExpirationMs = jwtRefreshExpirationMs;
    }

    public String generateToken(UserAccount user) {
        Instant now = clock.instant();
        List<String> roleNames = user.getRoles().stream()
                .map(Role::name)
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(now.plusMillis(jwtExpirationMs))
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .claim("roles", roleNames)
                .claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_ACCESS)
                .claim(TOKEN_VERSION_CLAIM, user.getTokenVersion())
                .build();

        return encoder.encode(JwtEncoderParameters.from(HS256_HEADER, claims)).getTokenValue();
    }

    public String generateRefreshToken(UserAccount user) {
        Instant now = clock.instant();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(now.plusMillis(jwtRefreshExpirationMs))
                .claim("userId", user.getId())
                .claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_REFRESH)
                .claim(TOKEN_VERSION_CLAIM, user.getTokenVersion())
                .build();

        return encoder.encode(JwtEncoderParameters.from(HS256_HEADER, claims)).getTokenValue();
    }

    public Long getUserIdFromToken(String token) {
        Object userId = decoder.decode(token).getClaim("userId");
        return userId instanceof Number n ? n.longValue() : null;
    }

    public boolean isRefreshToken(String token) {
        try {
            return TOKEN_TYPE_REFRESH.equals(decoder.decode(token).getClaimAsString(TOKEN_TYPE_CLAIM));
        } catch (JwtException e) {
            return false;
        }
    }

    public boolean validateToken(String authToken) {
        try {
            decoder.decode(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }

    public long getRefreshExpirationMs() {
        return jwtRefreshExpirationMs;
    }

    public String getEmailFromToken(String token) {
        return decoder.decode(token).getClaimAsString("email");
    }

    public List<String> getRolesFromToken(String token) {
        return decoder.decode(token).getClaimAsStringList("roles");
    }

    public int getTokenVersionFromToken(String token) {
        Object v = decoder.decode(token).getClaim(TOKEN_VERSION_CLAIM);
        return v instanceof Number n ? n.intValue() : 0;
    }
}

package com.vamigo.config.security;

import com.vamigo.auth.AppPrincipal;
import com.vamigo.user.Role;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class JwtClaimsExtractor {

    private JwtClaimsExtractor() {}

    public static AppPrincipal extractPrincipal(Jwt jwt) {
        Long userId = extractUserId(jwt);
        String email = extractEmail(jwt);
        Set<Role> roles = extractRoles(jwt);
        return new AppPrincipal(userId, email, roles);
    }

    public static AppJwtAuthenticationToken buildAuthentication(Jwt jwt) {
        AppPrincipal principal = extractPrincipal(jwt);
        var authorities = principal.roles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
                .toList();
        return new AppJwtAuthenticationToken(jwt, principal, authorities);
    }

    public static void validateAccessToken(Jwt jwt) {
        String tokenType = jwt.getClaimAsString("type");
        if (!"access".equals(tokenType)) {
            throw new InvalidBearerTokenException("Only access tokens are allowed for API access");
        }
    }

    private static Long extractUserId(Jwt jwt) {
        Object userId = jwt.getClaim("userId");
        if (userId == null) {
            throw new InvalidBearerTokenException("Token missing required 'userId' claim");
        }
        if (userId instanceof Number num) {
            return num.longValue();
        }
        try {
            return Long.parseLong(userId.toString());
        } catch (NumberFormatException e) {
            throw new InvalidBearerTokenException("Invalid userId in token");
        }
    }

    private static String extractEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            throw new InvalidBearerTokenException("Token missing required 'email' claim");
        }
        return email;
    }

    @SuppressWarnings("unchecked")
    private static Set<Role> extractRoles(Jwt jwt) {
        Object rolesObj = jwt.getClaim("roles");
        if (rolesObj == null) {
            throw new InvalidBearerTokenException("Token missing required 'roles' claim");
        }

        Set<Role> roles = new HashSet<>();
        if (rolesObj instanceof List<?> roleList) {
            for (Object roleName : roleList) {
                try {
                    roles.add(Role.valueOf(roleName.toString()));
                } catch (IllegalArgumentException e) {
                    throw new InvalidBearerTokenException("Invalid role in token: " + roleName);
                }
            }
        }

        if (roles.isEmpty()) {
            throw new InvalidBearerTokenException("Token missing required 'roles' claim");
        }
        return roles;
    }
}

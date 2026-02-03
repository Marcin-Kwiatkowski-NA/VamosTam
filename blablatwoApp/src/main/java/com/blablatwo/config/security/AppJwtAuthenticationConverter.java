package com.blablatwo.config.security;

import com.blablatwo.auth.AppPrincipal;
import com.blablatwo.user.Role;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AppJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLES = "roles";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String tokenType = jwt.getClaimAsString(CLAIM_TYPE);
        if (!TYPE_ACCESS.equals(tokenType)) {
            throw new InvalidBearerTokenException("Only access tokens are allowed for API access");
        }

        Long userId = extractUserId(jwt);

        String email = jwt.getClaimAsString(CLAIM_EMAIL);
        if (email == null) {
            throw new InvalidBearerTokenException("Token missing required 'email' claim");
        }

        Set<Role> roles = extractRoles(jwt);
        if (roles.isEmpty()) {
            throw new InvalidBearerTokenException("Token missing required 'roles' claim");
        }

        var authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
                .toList();

        AppPrincipal principal = new AppPrincipal(userId, email, roles);

        return new AppJwtAuthenticationToken(jwt, principal, authorities);
    }

    private Long extractUserId(Jwt jwt) {
        Object userId = jwt.getClaim(CLAIM_USER_ID);
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

    @SuppressWarnings("unchecked")
    private Set<Role> extractRoles(Jwt jwt) {
        Object rolesObj = jwt.getClaim(CLAIM_ROLES);
        if (rolesObj == null) {
            return Set.of();
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
        return roles;
    }
}

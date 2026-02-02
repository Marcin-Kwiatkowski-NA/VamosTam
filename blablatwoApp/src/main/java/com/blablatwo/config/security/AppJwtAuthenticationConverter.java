package com.blablatwo.config.security;

import com.blablatwo.auth.AppPrincipal;
import com.blablatwo.traveler.Role;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AppJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String CLAIM_TRAVELER_ID = "travelerId";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String tokenType = jwt.getClaimAsString(CLAIM_TYPE);
        if (!TYPE_ACCESS.equals(tokenType)) {
            throw new InvalidBearerTokenException("Only access tokens are allowed for API access");
        }

        Long travelerId = extractTravelerId(jwt);

        String email = jwt.getClaimAsString(CLAIM_EMAIL);
        if (email == null) {
            throw new InvalidBearerTokenException("Token missing required 'email' claim");
        }

        String roleName = jwt.getClaimAsString(CLAIM_ROLE);
        if (roleName == null) {
            throw new InvalidBearerTokenException("Token missing required 'role' claim");
        }

        Role role;
        try {
            role = Role.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            throw new InvalidBearerTokenException("Invalid role in token: " + roleName);
        }

        var authorities = List.of(new SimpleGrantedAuthority(role.getAuthority()));
        AppPrincipal principal = new AppPrincipal(travelerId, email, role);

        return new AppJwtAuthenticationToken(jwt, principal, authorities);
    }

    private Long extractTravelerId(Jwt jwt) {
        Object travelerId = jwt.getClaim(CLAIM_TRAVELER_ID);
        if (travelerId == null) {
            throw new InvalidBearerTokenException("Token missing required 'travelerId' claim");
        }
        if (travelerId instanceof Number num) {
            return num.longValue();
        }
        try {
            return Long.parseLong(travelerId.toString());
        } catch (NumberFormatException e) {
            throw new InvalidBearerTokenException("Invalid travelerId in token");
        }
    }
}

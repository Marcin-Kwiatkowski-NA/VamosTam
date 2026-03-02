package com.vamigo.config.security;

import com.vamigo.auth.AppPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;

import java.util.Collection;
import java.util.Map;

public class AppJwtAuthenticationToken extends AbstractOAuth2TokenAuthenticationToken<Jwt> {

    private final AppPrincipal principal;

    public AppJwtAuthenticationToken(Jwt jwt, AppPrincipal principal,
                                     Collection<? extends GrantedAuthority> authorities) {
        super(jwt, authorities);
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public String getName() {
        return principal.userId().toString();
    }

    @Override
    public AppPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public Map<String, Object> getTokenAttributes() {
        return getToken().getClaims();
    }
}

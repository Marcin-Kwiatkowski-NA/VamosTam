package com.blablatwo.auth.filter;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final String principal;

    public ApiKeyAuthenticationToken() {
        super(List.of(new SimpleGrantedAuthority("ROLE_EXTERNAL_SERVICE")));
        this.principal = "external-service";
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}

package com.vamigo.user;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    USER,
    CARRIER,
    ADMIN,
    SYSTEM;

    @Override
    public String getAuthority() {
        return "ROLE_" + name();
    }
}

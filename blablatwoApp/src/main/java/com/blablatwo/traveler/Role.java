package com.blablatwo.traveler;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {

    DRIVER, PASSENGER, ADMIN;

    @Override
    public String getAuthority() {
        // This is the format Spring Security expects for hasRole("DRIVER")
        return "ROLE_" + name();
    }
}
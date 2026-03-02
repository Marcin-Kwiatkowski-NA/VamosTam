package com.vamigo.auth;

import com.vamigo.user.Role;

import java.util.Set;

public record AppPrincipal(Long userId, String email, Set<Role> roles) {

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }
}

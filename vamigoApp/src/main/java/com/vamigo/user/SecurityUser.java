package com.vamigo.user;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;

public class SecurityUser implements UserDetails, CredentialsContainer {

    private final UserAccount account;
    private final Clock clock;
    private String passwordHash;

    public SecurityUser(UserAccount account, Clock clock) {
        this.account = account;
        this.clock = clock;
        this.passwordHash = account.getPasswordHash();
    }

    public Long getUserId() {
        return account.getId();
    }

    public UserAccount getAccount() {
        return account;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return account.getRoles();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return account.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return account.getStatus() != AccountStatus.BANNED
                && !account.isTemporarilyLocked(Instant.now(clock));
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return account.getStatus() == AccountStatus.ACTIVE;
    }

    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }
}

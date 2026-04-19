package com.vamigo.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@RequiredArgsConstructor
public class UserSecurityService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email.toLowerCase();
        return userAccountRepository.findByEmail(normalizedEmail)
                .map(account -> new SecurityUser(account, clock))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}

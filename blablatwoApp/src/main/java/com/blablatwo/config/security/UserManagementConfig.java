package com.blablatwo.config.security;

import com.blablatwo.traveler.TravelerRepository;
import com.blablatwo.traveler.user.TravelerSecurityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


@Configuration
public class UserManagementConfig {

    @Bean
    public UserDetailsService userDetailsService(TravelerRepository travelerRepository) {
        return new TravelerSecurityService(travelerRepository);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(4);
    }
}

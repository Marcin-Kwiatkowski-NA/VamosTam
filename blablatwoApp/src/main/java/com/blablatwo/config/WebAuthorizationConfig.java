package com.blablatwo.config;

import com.blablatwo.auth.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class WebAuthorizationConfig {

    private final CustomAuthenticationFailureHandler authenticationFailureHandler;
    private final CustomAuthenticationSuccessHandler authenticationSuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public WebAuthorizationConfig(CustomAuthenticationFailureHandler authenticationFailureHandler,
                                  CustomAuthenticationSuccessHandler authenticationSuccessHandler,
                                  JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain configure(HttpSecurity http) throws Exception {

        // Keep existing form login for web
        http.formLogin(c ->
                c.successHandler(authenticationSuccessHandler)
                        .failureHandler(authenticationFailureHandler)
        );

        // Add JWT filter for API/mobile authentication
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Authorization rules
        http.authorizeHttpRequests(c -> c
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/cities", "/cities/**").permitAll()
                .requestMatchers("/rides/search").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .anyRequest().permitAll()  // TODO: Change to authenticated() when ready
        );

        // Disable CSRF for API endpoints
        http.csrf(c -> c.disable());

        // Allow frames for H2 console
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        // Session management - support both stateful (web) and stateless (API)
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
        );

        return http.build();
    }
}

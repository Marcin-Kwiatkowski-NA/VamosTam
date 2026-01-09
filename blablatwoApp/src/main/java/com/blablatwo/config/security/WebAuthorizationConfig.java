package com.blablatwo.config.security;

import com.blablatwo.auth.filter.JwtAuthenticationFilter;
import com.blablatwo.config.CustomAuthenticationFailureHandler;
import com.blablatwo.config.CustomAuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class WebAuthorizationConfig {

    // NOT_USED: Form login handlers kept for potential future web UI
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
    SecurityFilterChain configure(HttpSecurity http) throws Exception {
        // NOT_USED: Form login for potential future web UI - see doc/security.md
        http.formLogin(c ->
                c.successHandler(authenticationSuccessHandler)
                        .failureHandler(authenticationFailureHandler)
        );

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/cities", "/cities/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/rides/search").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}

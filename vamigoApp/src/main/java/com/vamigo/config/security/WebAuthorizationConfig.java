package com.vamigo.config.security;

import com.vamigo.auth.AuthSecurityProperties;
import com.vamigo.auth.filter.ApiKeyAuthenticationFilter;
import com.vamigo.auth.filter.IpRateLimitFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({ApiKeyProperties.class, AuthSecurityProperties.class})
public class WebAuthorizationConfig {

    private final ApiKeyProperties apiKeyProperties;
    private final AuthSecurityProperties authSecurityProperties;
    private final AppJwtAuthenticationConverter appJwtAuthenticationConverter;

    public WebAuthorizationConfig(ApiKeyProperties apiKeyProperties,
                                  AuthSecurityProperties authSecurityProperties,
                                  AppJwtAuthenticationConverter appJwtAuthenticationConverter) {
        this.apiKeyProperties = apiKeyProperties;
        this.authSecurityProperties = authSecurityProperties;
        this.appJwtAuthenticationConverter = appJwtAuthenticationConverter;
    }

    @Bean
    @Order(1)
    SecurityFilterChain externalRidesSecurityFilterChain(HttpSecurity http) throws Exception {
        ApiKeyAuthenticationFilter apiKeyFilter = new ApiKeyAuthenticationFilter(apiKeyProperties.externalRides());

        http
            .securityMatcher("/rides/external/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain externalSeatsSecurityFilterChain(HttpSecurity http) throws Exception {
        ApiKeyAuthenticationFilter apiKeyFilter = new ApiKeyAuthenticationFilter(apiKeyProperties.externalRides());

        http
            .securityMatcher("/seats/external/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        var ipRateLimitFilter = new IpRateLimitFilter(
                authSecurityProperties.rateLimit().requestsPerMinute());

        http.formLogin(AbstractHttpConfigurer::disable);

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/hello").permitAll()
                .requestMatchers("/ws").permitAll()
                .requestMatchers("/auth/login").permitAll()
                .requestMatchers("/auth/register").permitAll()
                .requestMatchers("/auth/register/carrier").permitAll()
                .requestMatchers("/auth/google").permitAll()
                .requestMatchers("/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.GET, "/auth/verify-email").permitAll()
                .requestMatchers("/auth/forgot-password").permitAll()
                .requestMatchers("/auth/reset-password").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/privacy-policy.html").permitAll()
                .requestMatchers(HttpMethod.GET, "/terms.html").permitAll()
                .requestMatchers(HttpMethod.GET, "/carrier-profile/{userId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/rides/search").permitAll()
                .requestMatchers(HttpMethod.GET, "/seats/search").permitAll()
                .requestMatchers(HttpMethod.GET, "/search/config").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(appJwtAuthenticationConverter))
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .addFilterBefore(ipRateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("https://vamigo.app", "https://ac.vamigo.app",
                "https://s3.vamigo.app", "https://storage.vamigo.app", "http://localhost:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

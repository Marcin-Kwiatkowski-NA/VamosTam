package com.blablatwo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebAuthorizationConfig {

    private final CustomAuthenticationFailureHandler authenticationFailureHandler;
    private final CustomAuthenticationSuccessHandler authenticationSuccessHandler;

    public WebAuthorizationConfig(CustomAuthenticationFailureHandler authenticationFailureHandler, CustomAuthenticationSuccessHandler authenticationSuccessHandler) {
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
    }

    @Bean
    SecurityFilterChain configure(HttpSecurity http) throws Exception {

//        http.formLogin(c -> c.defaultSuccessUrl("/home", true));
        http.formLogin(c ->
                c.successHandler(authenticationSuccessHandler)
                .failureHandler(authenticationFailureHandler)
        );

        http.httpBasic(Customizer.withDefaults());

        http.authorizeHttpRequests(
                c -> c.requestMatchers("/cities").hasRole("DRIVER")
                        .anyRequest().hasRole("ADMIN")
//                        .anyRequest().permitAll()
//                        .anyRequest().authenticated()
        );

        return http.build();
    }
}

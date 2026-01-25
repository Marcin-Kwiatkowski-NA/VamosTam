package com.blablatwo.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that authenticates requests using an API key from the X-API-Key header.
 * Not a @Component - instantiated manually in security config with the configured key.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    private final String validApiKey;

    public ApiKeyAuthenticationFilter(String validApiKey) {
        this.validApiKey = validApiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);

        if (StringUtils.hasText(apiKey) && apiKey.equals(validApiKey)) {
            ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken();
            SecurityContextHolder.getContext().setAuthentication(authentication);
            logger.debug("API key authentication successful for request to {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }
}

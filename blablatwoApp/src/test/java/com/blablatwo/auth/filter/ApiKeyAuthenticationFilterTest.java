package com.blablatwo.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationFilterTest {

    private static final String VALID_API_KEY = "test-api-key-12345";

    private ApiKeyAuthenticationFilter filter;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new ApiKeyAuthenticationFilter(VALID_API_KEY);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Valid API key sets authentication in SecurityContext")
    void validApiKey_setsAuthentication() throws ServletException, IOException {
        // Arrange
        request.addHeader("X-API-Key", VALID_API_KEY);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isInstanceOf(ApiKeyAuthenticationToken.class);
        assertThat(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo("external-service");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_EXTERNAL_SERVICE"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Missing API key header leaves SecurityContext empty")
    void missingApiKey_noAuthentication() throws ServletException, IOException {
        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Invalid API key leaves SecurityContext empty")
    void invalidApiKey_noAuthentication() throws ServletException, IOException {
        // Arrange
        request.addHeader("X-API-Key", "wrong-api-key");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Empty API key header leaves SecurityContext empty")
    void emptyApiKey_noAuthentication() throws ServletException, IOException {
        // Arrange
        request.addHeader("X-API-Key", "");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}

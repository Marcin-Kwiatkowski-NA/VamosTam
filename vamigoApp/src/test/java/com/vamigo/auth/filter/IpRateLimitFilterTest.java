package com.vamigo.auth.filter;

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

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IpRateLimitFilterTest {

    private static final int REQUESTS_PER_MINUTE = 5;

    private IpRateLimitFilter filter;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new IpRateLimitFilter(REQUESTS_PER_MINUTE);
        request = new MockHttpServletRequest();
        request.setRequestURI("/auth/login");
        request.setRemoteAddr("192.168.1.1");
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("Requests within limit pass through to filter chain")
    void withinLimit_passesThrough() throws ServletException, IOException {
        for (int i = 0; i < REQUESTS_PER_MINUTE; i++) {
            filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);
        }

        verify(filterChain, times(REQUESTS_PER_MINUTE)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Request exceeding limit returns 429")
    void exceedingLimit_returns429() throws ServletException, IOException {
        for (int i = 0; i < REQUESTS_PER_MINUTE; i++) {
            filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);
        }

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
    }

    @Test
    @DisplayName("429 response body is ProblemDetail JSON")
    void exceedingLimit_responseBodyIsProblemDetail() throws ServletException, IOException {
        for (int i = 0; i < REQUESTS_PER_MINUTE; i++) {
            filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);
        }

        filter.doFilterInternal(request, response, filterChain);

        String body = response.getContentAsString();
        assertThat(body).contains("\"status\":429");
        assertThat(body).contains("\"title\":\"Too Many Requests\"");
        assertThat(body).contains("\"instance\":\"/auth/login\"");
    }

    @Test
    @DisplayName("Non-auth requests are never rate limited")
    void nonAuthRequest_neverRateLimited() throws ServletException, IOException {
        request.setRequestURI("/rides/search");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("Auth requests are filtered")
    void authRequest_isFiltered() {
        request.setRequestURI("/auth/login");

        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    @DisplayName("Different IPs have independent rate limit buckets")
    void differentIps_independentBuckets() throws ServletException, IOException {
        // Exhaust limit for first IP
        for (int i = 0; i < REQUESTS_PER_MINUTE; i++) {
            filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);
        }

        // Second IP should still be allowed
        MockHttpServletRequest secondRequest = new MockHttpServletRequest();
        secondRequest.setRequestURI("/auth/login");
        secondRequest.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();

        filter.doFilterInternal(secondRequest, secondResponse, filterChain);

        verify(filterChain).doFilter(secondRequest, secondResponse);
    }

    @Test
    @DisplayName("Exhausted IP is blocked while other IP passes")
    void exhaustedIp_blockedWhileOtherPasses() throws ServletException, IOException {
        // Exhaust first IP
        for (int i = 0; i < REQUESTS_PER_MINUTE; i++) {
            filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);
        }

        // First IP should be blocked
        filter.doFilterInternal(request, response, filterChain);
        assertThat(response.getStatus()).isEqualTo(429);

        // Second IP should still pass
        MockHttpServletRequest secondRequest = new MockHttpServletRequest();
        secondRequest.setRequestURI("/auth/login");
        secondRequest.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();

        filter.doFilterInternal(secondRequest, secondResponse, filterChain);
        assertThat(secondResponse.getStatus()).isEqualTo(200);
    }
}

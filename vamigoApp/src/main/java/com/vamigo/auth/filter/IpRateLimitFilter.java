package com.vamigo.auth.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * IP-based rate limiter for authentication endpoints.
 * Not a @Component — instantiated manually in security config.
 */
public class IpRateLimitFilter extends OncePerRequestFilter {

    private final Cache<String, Bucket> buckets;
    private final int requestsPerMinute;

    public IpRateLimitFilter(int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(10))
                .maximumSize(100_000)
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = request.getRemoteAddr();
        Bucket bucket = buckets.get(clientIp, _ -> createBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            writeRateLimitResponse(response, request.getRequestURI());
        }
    }

    private Bucket createBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(requestsPerMinute, Duration.ofMinutes(1)))
                .build();
    }

    private void writeRateLimitResponse(HttpServletResponse response, String requestUri) throws IOException {
        response.setStatus(429);
        response.setContentType("application/problem+json");
        response.setHeader("Retry-After", "60");
        response.getWriter().write(
                """
                {"type":"about:blank","title":"Too Many Requests","status":429,\
                "detail":"Too many authentication requests. Please try again later.",\
                "instance":"%s"}""".formatted(requestUri));
    }
}

package com.vamigo.auth;

import com.jayway.jsonpath.JsonPath;
import com.vamigo.AbstractFullStackTest;
import com.vamigo.auth.filter.IpRateLimitFilter;
import com.vamigo.email.BrevoClient;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Register → verify email, refresh rotation, and rate limit.
 *
 * <p>Rate limit is overridden only here via {@link TestPropertySource}; all other ITs share
 * a laxer limit from {@code application-test.properties} and therefore share a single Spring
 * context. This class forks into its own context — expected (and called out in the plan).
 *
 * <p>{@code BrevoClient} is {@link MockitoBean}'d so no real HTTP is attempted. The plaintext
 * verification token is extracted from the captured {@code params} map since the service only
 * persists its hash.
 */
@TestPropertySource(properties = "auth.rate-limit.requests-per-minute=3")
class AuthFlowIT extends AbstractFullStackTest {

    @MockitoBean BrevoClient brevoClient;

    @Autowired UserAccountRepository userAccountRepository;

    @Autowired IpRateLimitFilter ipRateLimitFilter;

    @BeforeEach
    void resetRateLimit() {
        ipRateLimitFilter.reset();
    }

    @Test
    void register_sendsVerificationEmail_andTokenVerifiesAccount() {
        String email = "register-flow@test.local";
        String password = "password123";

        String body = """
                {"email":"%s","password":"%s","displayName":"Display"}
                """.formatted(email, password);

        assertThat(mvc.post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .hasStatus(HttpStatus.CREATED);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> paramsCap = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(brevoClient).sendTemplateEmail(
                org.mockito.ArgumentMatchers.eq(email),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyLong(),
                paramsCap.capture());

        String url = paramsCap.getValue().get("VERIFICATION_URL");
        assertThat(url).contains("token=");
        String plainToken = url.substring(url.indexOf("token=") + "token=".length());

        assertThat(mvc.get().uri("/auth/verify-email").param("token", plainToken))
                .hasStatusOk();

        UserAccount acc = userAccountRepository.findByEmail(email).orElseThrow();
        assertThat(acc.getEmailVerifiedAt()).isNotNull();
    }

    @Test
    void refresh_afterPasswordChange_rejectsOldRefreshToken() throws Exception {
        String email = "rotate@test.local";
        String password = "password123";

        String regBody = """
                {"email":"%s","password":"%s","displayName":"Rotator"}
                """.formatted(email, password);

        MvcTestResult reg = mvc.post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(regBody)
                .exchange();

        assertThat(reg).hasStatus(HttpStatus.CREATED);
        String responseBody = reg.getResponse().getContentAsString();
        String accessToken = JsonPath.read(responseBody, "$.accessToken");
        String oldRefresh = JsonPath.read(responseBody, "$.refreshToken");

        // Bump tokenVersion via password change.
        String changeBody = """
                {"currentPassword":"%s","newPassword":"newPassword123"}
                """.formatted(password);
        assertThat(mvc.post().uri("/auth/change-password")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(changeBody))
                .hasStatusOk();

        String refreshBody = """
                {"refreshToken":"%s"}
                """.formatted(oldRefresh);
        assertThat(mvc.post().uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rateLimit_after3Requests_returns429() {
        String body = """
                {"email":"noone@test.local","password":"nope-pwd"}
                """;

        for (int i = 0; i < 3; i++) {
            mvc.post().uri("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .exchange();
        }

        assertThat(mvc.post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS)
                .matches(r -> r.getResponse().getHeader("Retry-After") != null);
    }
}

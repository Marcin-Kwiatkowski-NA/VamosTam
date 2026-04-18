package com.vamigo.auth;

import com.google.firebase.auth.FirebaseToken;
import com.vamigo.AbstractFullStackTest;
import com.vamigo.auth.exception.InvalidTokenException;
import com.vamigo.auth.service.FirebaseTokenVerifier;
import com.vamigo.user.AccountStatus;
import com.vamigo.user.Role;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import com.vamigo.user.UserProfile;
import com.vamigo.user.UserProfileRepository;
import com.vamigo.user.UserStats;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FacebookAuthIT extends AbstractFullStackTest {

    @MockitoBean FirebaseTokenVerifier firebaseTokenVerifier;

    @Autowired UserAccountRepository userAccountRepository;
    @Autowired UserProfileRepository userProfileRepository;

    private static final List<String> TEST_EMAILS = List.of(
            "newuser@fb.local", "shared@fb.local", "fb-user@fb.local");

    @BeforeEach
    void cleanup() {
        deleteTestUsers();
    }

    @AfterEach
    void teardown() {
        deleteTestUsers();
    }

    private void deleteTestUsers() {
        for (String email : TEST_EMAILS) {
            userAccountRepository.findByEmail(email).ifPresent(acc -> {
                userProfileRepository.deleteById(acc.getId());
                userAccountRepository.delete(acc);
            });
        }
    }

    private FirebaseToken fbTokenWith(String email, String facebookId, String name) {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getEmail()).thenReturn(email);
        when(token.getName()).thenReturn(name);
        when(token.getClaims()).thenReturn(Map.of(
                "firebase", Map.of(
                        "sign_in_provider", "facebook.com",
                        "identities", Map.of("facebook.com", List.of(facebookId))
                )
        ));
        return token;
    }

    @Test
    void postFacebook_withValidToken_provisionsNewUser() {
        when(firebaseTokenVerifier.verify(eq("token-new")))
                .thenReturn(fbTokenWith("newuser@fb.local", "fb-100001", "New User"));

        assertThat(mvc.post().uri("/auth/facebook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"idToken":"token-new"}
                        """))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.accessToken").asString().isNotEmpty();

        UserAccount account = userAccountRepository.findByEmail("newuser@fb.local").orElseThrow();
        assertThat(account.getFacebookId()).isEqualTo("fb-100001");
        assertThat(account.getProviders()).containsOnly(AuthProvider.FACEBOOK);
        assertThat(account.getEmailVerifiedAt()).isNotNull();
    }

    @Test
    void postFacebook_withExistingEmailAccount_linksFacebookProvider() {
        UserAccount existing = userAccountRepository.save(UserAccount.builder()
                .email("shared@fb.local")
                .passwordHash("hash")
                .providers(new HashSet<>(Set.of(AuthProvider.EMAIL)))
                .status(AccountStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(Role.USER)))
                .build());
        userProfileRepository.save(UserProfile.builder()
                .account(existing)
                .displayName("Shared")
                .stats(new UserStats())
                .build());

        when(firebaseTokenVerifier.verify(eq("token-link")))
                .thenReturn(fbTokenWith("shared@fb.local", "fb-200002", "Shared"));

        assertThat(mvc.post().uri("/auth/facebook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"idToken":"token-link"}
                        """))
                .hasStatusOk();

        UserAccount linked = userAccountRepository.findById(existing.getId()).orElseThrow();
        assertThat(linked.getFacebookId()).isEqualTo("fb-200002");
        assertThat(linked.getProviders()).contains(AuthProvider.EMAIL, AuthProvider.FACEBOOK);
    }

    @Test
    void postFacebook_withExistingFacebookId_reusesAccount() {
        UserAccount existing = userAccountRepository.save(UserAccount.builder()
                .email("fb-user@fb.local")
                .facebookId("fb-300003")
                .providers(new HashSet<>(Set.of(AuthProvider.FACEBOOK)))
                .status(AccountStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(Role.USER)))
                .build());
        userProfileRepository.save(UserProfile.builder()
                .account(existing)
                .displayName("FB User")
                .stats(new UserStats())
                .build());

        when(firebaseTokenVerifier.verify(eq("token-reuse")))
                .thenReturn(fbTokenWith("fb-user@fb.local", "fb-300003", "FB User"));

        assertThat(mvc.post().uri("/auth/facebook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"idToken":"token-reuse"}
                        """))
                .hasStatusOk();

        // Reused, not freshly created
        assertThat(userAccountRepository.findByFacebookId("fb-300003").orElseThrow().getId())
                .isEqualTo(existing.getId());
    }

    @Test
    void postFacebook_withInvalidSignature_returns401() {
        when(firebaseTokenVerifier.verify(eq("token-bad")))
                .thenThrow(new InvalidTokenException("Invalid Firebase ID token"));

        assertThat(mvc.post().uri("/auth/facebook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"idToken":"token-bad"}
                        """))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void postFacebook_spoofedAsGoogle_returns401() {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getEmail()).thenReturn("spoof@fb.local");
        when(token.getClaims()).thenReturn(Map.of(
                "firebase", Map.of(
                        "sign_in_provider", "google.com",
                        "identities", Map.of("google.com", List.of("g-123"))
                )
        ));
        when(firebaseTokenVerifier.verify(eq("token-spoof"))).thenReturn(token);

        assertThat(mvc.post().uri("/auth/facebook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"idToken":"token-spoof"}
                        """))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .extractingPath("$.detail").asString().contains("Facebook");
    }

    @Test
    void postFacebook_missingFacebookIdentity_returns401() {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getEmail()).thenReturn("x@fb.local");
        when(token.getClaims()).thenReturn(Map.of(
                "firebase", Map.of(
                        "sign_in_provider", "facebook.com",
                        "identities", Map.of()
                )
        ));
        when(firebaseTokenVerifier.verify(eq("token-noid"))).thenReturn(token);

        assertThat(mvc.post().uri("/auth/facebook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"idToken":"token-noid"}
                        """))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void postFacebook_missingEmail_returns400WithErrorCode() {
        when(firebaseTokenVerifier.verify(eq("token-noemail")))
                .thenReturn(fbTokenWith(null, "fb-999", "No Email User"));

        assertThat(mvc.post().uri("/auth/facebook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"idToken":"token-noemail"}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code").asString().isEqualTo("MISSING_EMAIL_PERMISSION");
    }
}

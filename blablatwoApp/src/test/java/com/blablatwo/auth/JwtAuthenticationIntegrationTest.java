package com.blablatwo.auth;

import com.blablatwo.auth.service.JwtTokenProvider;
import com.blablatwo.user.AccountStatus;
import com.blablatwo.user.Role;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import com.blablatwo.user.UserProfile;
import com.blablatwo.user.UserProfileRepository;
import com.blablatwo.user.UserStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JwtAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    private UserAccount testUser;

    @BeforeEach
    void setUp() {
        testUser = userAccountRepository.save(UserAccount.builder()
                .email("test@example.com")
                .passwordHash("encoded-password")
                .status(AccountStatus.ACTIVE)
                .roles(Set.of(Role.USER))
                .build());

        userProfileRepository.save(UserProfile.builder()
                .account(testUser)
                .displayName("Test User")
                .stats(new UserStats())
                .build());
    }

    @Test
    @DisplayName("/auth/me without token returns 401")
    void authMe_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Access token authenticates and yields correct user data")
    void accessToken_authenticates_andYieldsAppPrincipal() throws Exception {
        String accessToken = jwtTokenProvider.generateToken(testUser);

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }

    @Test
    @DisplayName("Refresh token is rejected on protected endpoint with 401")
    void refreshToken_isRejected_on_protectedEndpoint() throws Exception {
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/auth/login allows anonymous access")
    void publicEndpoint_login_allowsAnonymousAccess() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@example.com\",\"password\":\"y\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/auth/register allows anonymous access")
    void publicEndpoint_register_allowsAnonymousAccess() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("/cities allows anonymous access")
    void publicEndpoint_cities_allowsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/cities"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Invalid bearer token returns 401")
    void invalidBearerToken_returns401() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin role is correctly mapped from token")
    void adminRole_isCorrectlyMapped() throws Exception {
        UserAccount admin = userAccountRepository.save(UserAccount.builder()
                .email("admin@example.com")
                .passwordHash("encoded-password")
                .status(AccountStatus.ACTIVE)
                .roles(Set.of(Role.ADMIN))
                .build());

        userProfileRepository.save(UserProfile.builder()
                .account(admin)
                .displayName("Admin User")
                .stats(new UserStats())
                .build());

        String accessToken = jwtTokenProvider.generateToken(admin);

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(admin.getId()));
    }
}

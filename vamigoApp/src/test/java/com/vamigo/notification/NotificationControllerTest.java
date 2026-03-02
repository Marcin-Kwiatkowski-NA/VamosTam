package com.vamigo.notification;

import com.vamigo.auth.AppPrincipal;
import com.vamigo.auth.service.JwtTokenProvider;
import com.vamigo.notification.dto.NotificationPageDto;
import com.vamigo.notification.dto.NotificationResponseDto;
import com.vamigo.notification.dto.UnreadCountDto;
import com.vamigo.user.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private NotificationService notificationService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static UsernamePasswordAuthenticationToken userAuth() {
        AppPrincipal p = new AppPrincipal(1L, "test@example.com", Set.of(Role.USER));
        return new UsernamePasswordAuthenticationToken(p, null, p.roles());
    }

    @Test
    @DisplayName("GET /me/notifications returns paginated feed")
    void getNotifications() throws Exception {
        var dto = new NotificationResponseDto(
                UUID.randomUUID(), NotificationType.BOOKING_CONFIRMED,
                NotificationChannel.BOOKING_UPDATES, EntityType.RIDE,
                "42", Map.of("offerKey", "r-42"), "booking:99",
                1, Instant.now(), null);
        var page = new NotificationPageDto(List.of(dto), false, 1L);

        when(notificationService.getNotifications(any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/me/notifications")
                        .with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications").isArray())
                .andExpect(jsonPath("$.notifications[0].type").value("BOOKING_CONFIRMED"))
                .andExpect(jsonPath("$.hasMore").value(false))
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    @DisplayName("GET /me/notifications/unread-count returns count")
    void getUnreadCount() throws Exception {
        when(notificationService.getUnreadCount(any())).thenReturn(new UnreadCountDto(3));

        mockMvc.perform(get("/me/notifications/unread-count")
                        .with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    @DisplayName("POST /me/notifications/{id}/read returns 204")
    void markRead() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/me/notifications/" + id + "/read")
                        .with(csrf())
                        .with(authentication(userAuth())))
                .andExpect(status().isNoContent());

        verify(notificationService).markRead(any(), any());
    }

    @Test
    @DisplayName("POST /me/notifications/read-all returns 204")
    void markAllRead() throws Exception {
        mockMvc.perform(post("/me/notifications/read-all")
                        .with(csrf())
                        .with(authentication(userAuth())))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllRead(any());
    }
}

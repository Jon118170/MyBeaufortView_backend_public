package com.mybeaufortviewproject.mybeaufortview_backend.notification;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.realtime.RealtimeNotificationService;
import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.UserPrincipal;
import com.mybeaufortviewproject.mybeaufortview_backend.notification.dto.NotificationResponse;

@WebMvcTest(NotificationController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private RealtimeNotificationService realtimeNotificationService;

    private final Long userId = 1L;

    @BeforeEach
    public void setUp() {
        UserPrincipal userPrincipal = new UserPrincipal(userId, "test@example.com", "USER");
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userPrincipal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void getNotifications_shouldReturnNotificationList() throws Exception {
        NotificationResponse notification = new NotificationResponse(
                10L,
                NotificationType.THUMBNAIL_READY,
                "Thumbnail ready",
                "Your photo thumbnail has been generated.",
                false,
                42L,
                null,
                Instant.parse("2026-04-02T12:00:00Z"),
                null
        );

        when(notificationService.getNotificationsForUser(userId))
                .thenReturn(List.of(notification));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].type").value("THUMBNAIL_READY"))
                .andExpect(jsonPath("$[0].title").value("Thumbnail ready"))
                .andExpect(jsonPath("$[0].message").value("Your photo thumbnail has been generated."))
                .andExpect(jsonPath("$[0].read").value(false))
                .andExpect(jsonPath("$[0].postId").value(42));

        verify(notificationService).getNotificationsForUser(userId);
    }

    @Test
    public void getUnreadCount_shouldReturnUnreadCount() throws Exception {
        when(notificationService.getUnreadCountForUser(userId)).thenReturn(3L);

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));

        verify(notificationService).getUnreadCountForUser(userId);
    }

    @Test
    public void markAsRead_shouldReturnNoContent() throws Exception {
        Long notificationId = 99L;

        mockMvc.perform(patch("/api/notifications/{id}/read", notificationId))
                .andExpect(status().isNoContent());

        verify(notificationService).markAsRead(notificationId, userId);
    }
}

package com.mybeaufortviewproject.mybeaufortview_backend.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.ErrorCode;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.NotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.realtime.RealtimeNotificationService;
import com.mybeaufortviewproject.mybeaufortview_backend.notification.dto.NotificationResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.user.Role;
import com.mybeaufortviewproject.mybeaufortview_backend.user.User;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private RealtimeNotificationService realtimeNotificationService;

    @InjectMocks
    private NotificationService notificationService;


    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    private User user;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("photographer1");
        user.setName("Photographer One");
        user.setEmail("photographer@example.com");
        user.setPassword("password");
        user.setRole(Role.PRIVILEGED_USER);
    }

    @Test
    public void createNotification_shouldPersistUnreadNotification() {
       when(notificationRepository.save(any(Notification.class)))
               .thenAnswer(invocation -> {
                   Notification notification = invocation.getArgument(0);
                   notification.setId(1L); // Simulate database generated ID
                   notification.setCreatedAt(Instant.parse("2026-04-01T12:00:00Z"));
                   return notification;
           });


        NotificationResponse response = notificationService.createNotification(
                user,
                NotificationType.THUMBNAIL_READY,
                "Thumbnail ready",
                "Your photo thumbnail has been generated.",
                42L,
                null
        );

        verify(notificationRepository).save(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();

        assertEquals(user, saved.getUser());
        assertEquals(NotificationType.THUMBNAIL_READY, saved.getType());
        assertEquals("Thumbnail ready", saved.getTitle());
        assertEquals("Your photo thumbnail has been generated.", saved.getMessage());
        assertFalse(saved.isRead());
        assertEquals(42L, saved.getPostId());
        assertNull(saved.getCommissionRequestId());
        assertNull(saved.getReadAt());

        assertEquals(1L, response.id());
        assertEquals(NotificationType.THUMBNAIL_READY, response.type());
        assertEquals("Thumbnail ready", response.title());
        assertEquals("Your photo thumbnail has been generated.", response.message());
        assertFalse(response.read());
        assertEquals(42L, response.postId());
        assertNull(response.commissionRequestId());
        assertNotNull(response.createdAt());

        verify(realtimeNotificationService).sendNotification(eq(user.getId()), any(NotificationResponse.class));
    }

    @Test
    public void getNotificationsForUser_shouldReturnMappedNotificationsOrderedDesc() {
        Notification notification = new Notification();
        notification.setId(10L);
        notification.setUser(user);
        notification.setType(NotificationType.AI_TAGGING_COMPLETED);
        notification.setTitle("AI tagging completed");
        notification.setMessage("AI tags have been generated for your photo.");
        notification.setRead(false);
        notification.setPostId(99L);
        notification.setCommissionRequestId(null);
        notification.setCreatedAt(Instant.parse("2026-04-02T12:00:00Z"));

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(notification));

        List<NotificationResponse> result = notificationService.getNotificationsForUser(1L);

        assertEquals(1, result.size());

        NotificationResponse response = result.get(0);
        assertEquals(10L, response.id());
        assertEquals(NotificationType.AI_TAGGING_COMPLETED, response.type());
        assertEquals("AI tagging completed", response.title());
        assertEquals("AI tags have been generated for your photo.", response.message());
        assertFalse(response.read());
        assertEquals(99L, response.postId());
        assertNull(response.commissionRequestId());
        assertEquals(Instant.parse("2026-04-02T12:00:00Z"), response.createdAt());
    }

    @Test
    public void getUnreadCountForUser_shouldReturnUnreadCount() {
        when(notificationRepository.countByUserIdAndReadFalse(1L)).thenReturn(3L);

        long count = notificationService.getUnreadCountForUser(1L);

        assertEquals(3L, count);
    }

    @Test
    public void markAsRead_shouldSetReadTrueAndReadAt() {
        Notification notification = new Notification();
        notification.setId(5L);
        notification.setUser(user);
        notification.setRead(false);

        when(notificationRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.of(notification));

        notificationService.markAsRead(5L, 1L);

        assertTrue(notification.isRead());
        assertNotNull(notification.getReadAt());
    }

    @Test
    public void markAsRead_shouldThrowWhenNotificationNotFoundForUser() {
        when(notificationRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> notificationService.markAsRead(5L, 1L)
        );

        assertEquals(
                ErrorCode.NOTIFICATION_NOT_FOUND,
                ex.getCode()
        );
    }
}

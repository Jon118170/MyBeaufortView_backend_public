package com.mybeaufortviewproject.mybeaufortview_backend.notification;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.realtime.RealtimeNotificationService;
import com.mybeaufortviewproject.mybeaufortview_backend.notification.dto.NotificationResponse;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class RealtimeNotificationServiceTest {
    @Mock
    private NotificationRepository notificationRepository;

    private RealtimeNotificationService realtimeNotificationService;

    @BeforeEach
    public void setUp() {
        realtimeNotificationService = new RealtimeNotificationService(notificationRepository);
    }

    @Test
    public void subscribe_shouldReplayMissedNotificationsWhenLastEventIdProvided() {
        Notification notification1 = new Notification();
        notification1.setId(4L);
        notification1.setType(NotificationType.PHOTO_LIKED);
        notification1.setTitle("New like");
        notification1.setMessage("user1 liked your post.");
        notification1.setRead(false);

        Notification notification2 = new Notification();
        notification2.setId(5L);
        notification2.setType(NotificationType.COMMISSION_REQUEST_RECEIVED);
        notification2.setTitle("New commission request");
        notification2.setMessage("user2 sent you a commission request.");
        notification2.setRead(false);

        when(notificationRepository.findByUserIdAndIdGreaterThanOrderByIdAsc(1L, 3L))
                .thenReturn(List.of(notification1, notification2));

        SseEmitter emitter = realtimeNotificationService.subscribe(1L, 3L);

        assertNotNull(emitter);
        verify(notificationRepository).findByUserIdAndIdGreaterThanOrderByIdAsc(1L, 3L);
    }

    @Test
    public void subscribe_shouldNotReplayWhenLastEventIdIsNull() {
        SseEmitter emitter = realtimeNotificationService.subscribe(1L, null);

        assertNotNull(emitter);
        verify(notificationRepository, never())
                .findByUserIdAndIdGreaterThanOrderByIdAsc(anyLong(), anyLong());
    }

    @Test
    public void sendNotification_shouldDoNothingWhenNoEmittersExist() {
        NotificationResponse response = new NotificationResponse(
                10L,
                NotificationType.PHOTO_LIKED,
                "New like",
                "user1 liked your post.",
                false,
                1L,
                null,
                Instant.now(),
                null
        );

        assertDoesNotThrow(() -> realtimeNotificationService.sendNotification(1L, response));
    }
}

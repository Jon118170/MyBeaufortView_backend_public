package com.mybeaufortviewproject.mybeaufortview_backend.notification;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.realtime.RealtimeNotificationService;
import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.UserPrincipal;
import com.mybeaufortviewproject.mybeaufortview_backend.notification.dto.NotificationResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.notification.dto.UnreadCountResponse;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final RealtimeNotificationService realtimeNotificationService;

    public NotificationController(NotificationService notificationService, RealtimeNotificationService realtimeNotificationService) {
        this.notificationService = notificationService;
        this.realtimeNotificationService = realtimeNotificationService;
    }

    @GetMapping
    public List<NotificationResponse> getNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return notificationService.getNotificationsForUser(userPrincipal.id());
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return new UnreadCountResponse(
                notificationService.getUnreadCountForUser(userPrincipal.id())
        );
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) Long lastEventId
    ) {
        return realtimeNotificationService.subscribe(userPrincipal.id(), lastEventId);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        notificationService.markAsRead(id, userPrincipal.id());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        notificationService.deleteNotification(id, userPrincipal.id());
        return ResponseEntity.noContent().build();
    }

}

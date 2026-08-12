package com.mybeaufortviewproject.mybeaufortview_backend.notification;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.ErrorCode;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.NotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.realtime.RealtimeNotificationService;
import com.mybeaufortviewproject.mybeaufortview_backend.notification.dto.NotificationResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.user.User;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    private final RealtimeNotificationService realtimeNotificationService;

    public NotificationService(NotificationRepository notificationRepository, RealtimeNotificationService realtimeNotificationService) {
        this.notificationRepository = notificationRepository;
        this.realtimeNotificationService = realtimeNotificationService;
    }

    public NotificationResponse createNotification(
            User user,
            NotificationType type,
            String title,
            String message,
            Long postId,
            Long commissionRequestId
    ) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setReadAt(null);
        notification.setPostId(postId);
        notification.setCommissionRequestId(commissionRequestId);

        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = toNotificationResponse(saved);

        realtimeNotificationService.sendNotification(user.getId(), response);

        return response;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toNotificationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCountForUser(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException(
                                ErrorCode.NOTIFICATION_NOT_FOUND,
                                "Notification not found with id: " + notificationId
        ));
        notificationRepository.delete(notification);
    }

    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException(
                                ErrorCode.NOTIFICATION_NOT_FOUND,
                                "Notification not found with id: " + notificationId
    ));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());

        }
    }

    private NotificationResponse toNotificationResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getPostId(),
                notification.getCommissionRequestId(),
                notification.getCreatedAt(),
                notification.getReadAt()
            );
    }
}

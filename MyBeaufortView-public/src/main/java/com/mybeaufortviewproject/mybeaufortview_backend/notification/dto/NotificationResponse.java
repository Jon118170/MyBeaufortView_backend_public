package com.mybeaufortviewproject.mybeaufortview_backend.notification.dto;

import java.time.Instant;

import com.mybeaufortviewproject.mybeaufortview_backend.notification.NotificationType;

public record NotificationResponse (
    Long id,
    NotificationType type,
    String title,
    String message,
    boolean read,
    Long postId,
    Long commissionRequestId,
    Instant createdAt,
    Instant readAt
) {

}

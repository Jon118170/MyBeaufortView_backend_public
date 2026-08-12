package com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.realtime;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.mybeaufortviewproject.mybeaufortview_backend.notification.Notification;
import com.mybeaufortviewproject.mybeaufortview_backend.notification.NotificationRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.notification.dto.NotificationResponse;

@Service
public class RealtimeNotificationService {

    private static final long SSE_TIMEOUT = 0L;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    private final NotificationRepository notificationRepository;

    public RealtimeNotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public SseEmitter subscribe(Long userId, Long lastEventId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitters.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(ex -> removeEmitter(userId, emitter));

        sendConnectionEvent(userId, emitter);

        if (lastEventId != null) {
            List<Notification> missed = notificationRepository
                    .findByUserIdAndIdGreaterThanOrderByIdAsc(userId, lastEventId);

            for (Notification notification : missed) {
                try {
                    NotificationResponse response = toNotificationResponse(notification);

                    emitter.send(SseEmitter.event()
                            .id(String.valueOf(response.id()))
                            .name("notification")
                            .data(response));
                } catch (IOException e) {
                    removeEmitter(userId, emitter);
                    break;
                }
            }
        }


        return emitter;
    }

    public void sendNotification(Long userId, NotificationResponse notification) {

        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(notification.id()))
                        .name("notification")
                        .data(notification));
            } catch (IOException ex) {
                removeEmitter(userId, emitter);
            }
        }
    }

    private void sendConnectionEvent(Long userId, SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Notification stream connected"));
        } catch (IOException ex) {
            removeEmitter(userId, emitter);
        }

    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) {
            return;
        }

        userEmitters.remove(emitter);

        if (userEmitters.isEmpty()) {
            emitters.remove(userId);
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

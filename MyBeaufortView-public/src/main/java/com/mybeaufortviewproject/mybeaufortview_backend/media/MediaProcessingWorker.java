package com.mybeaufortviewproject.mybeaufortview_backend.media;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.storage.ThumbnailService;
import com.mybeaufortviewproject.mybeaufortview_backend.notification.NotificationService;
import com.mybeaufortviewproject.mybeaufortview_backend.notification.NotificationType;


@Service
public class MediaProcessingWorker {

    private static final Logger log = LoggerFactory.getLogger(MediaProcessingWorker.class);

    private final MediaJobRepository mediaJobRepository;
    private final MediaJobService mediaJobService;
    private final PhotoTaggingService photoTaggingService;
    private final ThumbnailService thumbnailService;
    private final NotificationService notificationService;

    public MediaProcessingWorker(MediaJobRepository mediaJobRepository, MediaJobService mediaJobService,
            PhotoTaggingService photoTaggingService, ThumbnailService thumbnailService, NotificationService notificationService) {
        this.mediaJobRepository = mediaJobRepository;
        this.mediaJobService = mediaJobService;
        this.photoTaggingService = photoTaggingService;
        this.thumbnailService = thumbnailService;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 15000)
    public void processPendingJobs() {
        List<MediaJob> pendingJobs =
                mediaJobRepository.findPendingWithPost(MediaJobStatus.PENDING);

        for (MediaJob job : pendingJobs) {
            try {
                processJob(job);
            } catch (Exception e) {
                log.error("Unhandled error processing job id={} type={}", job.getId(), job.getJobType(), e);
            }
        }
    }

    public void processJob(MediaJob job) {
        try {
            mediaJobService.markProcessing(job.getId());

            if (job.getJobType() == MediaJobType.THUMBNAIL_GENERATION) {
                thumbnailService.generateThumbnailForPost(job.getPost());
            } else if (job.getJobType() == MediaJobType.AI_TAGGING) {
                photoTaggingService.refreshTagsForPost(job.getPost());
            } else {
                throw new IllegalArgumentException("Unsupported job type: " + job.getJobType());
            }

            mediaJobService.markCompleted(job.getId());

            try {
                createSuccessNotification(job);
            } catch (Exception e) {
                log.warn("Failed to send success notification for job id={}", job.getId(), e);
            }

        } catch (Exception e) {
            log.error("Job id={} type={} failed: {}", job.getId(), job.getJobType(), e.getMessage(), e);
            try {
                mediaJobService.markFailed(job.getId(), e.getMessage());
            } catch (Exception ex) {
                log.error("Failed to mark job {} as failed", job.getId(), ex);
            }
            try {
                createFailureNotification(job, e.getMessage());
            } catch (Exception ex) {
                log.warn("Failed to send failure notification for job id={}", job.getId(), ex);
            }
        }
    }

    private void createSuccessNotification(MediaJob job) {
        if (job.getJobType() == MediaJobType.THUMBNAIL_GENERATION) {
            notificationService.createNotification(
                    job.getPost().getUser(),
                    NotificationType.THUMBNAIL_READY,
                    "Thumbnail ready",
                    "Your photo thumbnail has been generated.",
                    job.getPost().getId(),
                    null
            );
        } else if (job.getJobType() == MediaJobType.AI_TAGGING) {
            notificationService.createNotification(
                    job.getPost().getUser(),
                    NotificationType.AI_TAGGING_COMPLETED,
                    "AI tagging completed",
                    "AI tags have been generated for your photo.",
                    job.getPost().getId(),
                    null
                );
            }
        }

    private void createFailureNotification(MediaJob job, String errorMessage) {
        notificationService.createNotification(
                job.getPost().getUser(),
                NotificationType.MEDIA_PROCESSING_FAILED,
                "Media processing failed",
                "An error occurred while processing your media: " + errorMessage,
                job.getPost().getId(),
                null
        );
    }
}

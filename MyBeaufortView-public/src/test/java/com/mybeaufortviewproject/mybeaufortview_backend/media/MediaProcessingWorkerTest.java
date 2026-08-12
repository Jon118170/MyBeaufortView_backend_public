package com.mybeaufortviewproject.mybeaufortview_backend.media;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.storage.ThumbnailService;
import com.mybeaufortviewproject.mybeaufortview_backend.notification.NotificationService;
import com.mybeaufortviewproject.mybeaufortview_backend.notification.NotificationType;
import com.mybeaufortviewproject.mybeaufortview_backend.post.Post;
import com.mybeaufortviewproject.mybeaufortview_backend.user.User;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class MediaProcessingWorkerTest {

    @Mock
    private MediaJobRepository mediaJobRepository;

    @Mock
    private MediaJobService mediaJobService;

    @Mock
    private PhotoTaggingService photoTaggingService;

    @Mock
    private ThumbnailService thumbnailService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private MediaProcessingWorker mediaProcessingWorker;

    @Test
    public void processPendingJobs_shouldProcessAiTaggingJobs() {
        User user = new User();
        user.setId(100L);

        Post post = new Post();
        post.setId(1L);
        post.setUser(user);

        MediaJob job = mock(MediaJob.class);
        when(job.getId()).thenReturn(10L);
        when(job.getPost()).thenReturn(post);
        when(job.getJobType()).thenReturn(MediaJobType.AI_TAGGING);

        when(mediaJobRepository.findPendingWithPost(MediaJobStatus.PENDING))
                .thenReturn(List.of(job));

        mediaProcessingWorker.processPendingJobs();

        verify(mediaJobService).markProcessing(10L);
        verify(photoTaggingService).refreshTagsForPost(post);
        verify(mediaJobService).markCompleted(10L);
        verify(notificationService).createNotification(
                user,
                NotificationType.AI_TAGGING_COMPLETED,
                "AI tagging completed",
                "AI tags have been generated for your photo.",
                1L,
                null
        );
    }

    @Test
    public void processJob_shouldMarkFailedWhenTaggingThrows() {
        User user = new User();
        user.setId(100L);

        Post post = new Post();
        post.setId(1L);
        post.setUser(user);

        MediaJob job = mock(MediaJob.class);
        when(job.getId()).thenReturn(20L);
        when(job.getPost()).thenReturn(post);
        when(job.getJobType()).thenReturn(MediaJobType.AI_TAGGING);

        doThrow(new RuntimeException("AI tagging failed"))
                .when(photoTaggingService).refreshTagsForPost(post);

        mediaProcessingWorker.processJob(job);

        verify(mediaJobService).markProcessing(20L);
        verify(mediaJobService).markFailed(20L, "AI tagging failed");
        verify(notificationService).createNotification(
                user,
                NotificationType.MEDIA_PROCESSING_FAILED,
                "Media processing failed",
                "An error occurred while processing your media: AI tagging failed",
                1L,
                null
        );
    }

    @Test
    public void processPendingJobs_shouldProcessThumbnailJobs() {
        User user = new User();
        user.setId(100L);

        Post post = new Post();
        post.setId(1L);
        post.setUser(user);

        MediaJob job = mock(MediaJob.class);
        when(job.getId()).thenReturn(30L);
        when(job.getPost()).thenReturn(post);
        when(job.getJobType()).thenReturn(MediaJobType.THUMBNAIL_GENERATION);

        when(mediaJobRepository.findPendingWithPost(MediaJobStatus.PENDING))
                .thenReturn(List.of(job));

        mediaProcessingWorker.processPendingJobs();

        verify(mediaJobService).markProcessing(30L);
        verify(thumbnailService).generateThumbnailForPost(post);
        verify(mediaJobService).markCompleted(30L);
        verify(notificationService).createNotification(
                user,
                NotificationType.THUMBNAIL_READY,
                "Thumbnail ready",
                "Your photo thumbnail has been generated.",
                1L,
                null
            );
    }

    @Test
    public void processJob_shouldMarkFailedWhenThumbnailGenerationThrows() {
        User user = new User();
        user.setId(100L);

        Post post = new Post();
        post.setId(1L);
        post.setUser(user);

        MediaJob job = mock(MediaJob.class);
        when(job.getId()).thenReturn(40L);
        when(job.getPost()).thenReturn(post);
        when(job.getJobType()).thenReturn(MediaJobType.THUMBNAIL_GENERATION);

        doThrow(new RuntimeException("Thumbnail generation failed"))
                .when(thumbnailService).generateThumbnailForPost(post);

        mediaProcessingWorker.processJob(job);

        verify(mediaJobService).markProcessing(40L);
        verify(mediaJobService).markFailed(40L, "Thumbnail generation failed");
        verify(notificationService).createNotification(
                user,
                NotificationType.MEDIA_PROCESSING_FAILED,
                "Media processing failed",
                "An error occurred while processing your media: Thumbnail generation failed",
                1L,
                null
        );
    }
}

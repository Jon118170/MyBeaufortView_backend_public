package com.mybeaufortviewproject.mybeaufortview_backend.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.ErrorCode;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.NotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.media.dto.MediaJobStatusResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.media.dto.MediaStatusResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.post.Post;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class MediaJobServiceTest {

    @Mock
    private MediaJobRepository mediaJobRepository;

    @InjectMocks
    private MediaJobService mediaJobService;

    private Post post;
    private MediaJob mediaJob;

    @BeforeEach
    public void setUp() {
        post = new Post();
        post.setId(1L);

        mediaJob = new MediaJob();
        mediaJob.setPost(post);
        mediaJob.setJobType(MediaJobType.AI_TAGGING);
        mediaJob.setStatus(MediaJobStatus.PENDING);
        mediaJob.setAttemptCount(0);
    }

    @Test
    public void createJob_shouldCreatePendingJob() {
        when(mediaJobRepository.save(any(MediaJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MediaJob created = mediaJobService.createJob(post, MediaJobType.AI_TAGGING);

        assertThat(created.getPost()).isEqualTo(post);
        assertThat(created.getJobType()).isEqualTo(MediaJobType.AI_TAGGING);
        assertThat(created.getStatus()).isEqualTo(MediaJobStatus.PENDING);
        assertThat(created.getAttemptCount()).isEqualTo(0);
    }

    @Test
    public void markProcessing_shouldSetProcessingAndIncrementAttemptCount() {
        when(mediaJobRepository.findById(1L)).thenReturn(Optional.of(mediaJob));
        when(mediaJobRepository.save(any(MediaJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MediaJob updated = mediaJobService.markProcessing(1L);

        assertThat(updated.getStatus()).isEqualTo(MediaJobStatus.PROCESSING);
        assertThat(updated.getAttemptCount()).isEqualTo(1);
        assertThat(updated.getStartedAt()).isNotNull();
        assertThat(updated.getErrorMessage()).isNull();
    }

    @Test
    public void markCompleted_shouldSetCompletedAndClearErrorMessage() {
        mediaJob.setErrorMessage("Old error");

        when(mediaJobRepository.findById(1L)).thenReturn(Optional.of(mediaJob));
        when(mediaJobRepository.save(any(MediaJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MediaJob updated = mediaJobService.markCompleted(1L);

        assertThat(updated.getStatus()).isEqualTo(MediaJobStatus.COMPLETED);
        assertThat(updated.getCompletedAt()).isNotNull();
        assertThat(updated.getErrorMessage()).isNull();
    }

    @Test
    public void markFailed_shouldSetFailedAndStoreErrorMessage() {
        when(mediaJobRepository.findById(1L)).thenReturn(Optional.of(mediaJob));
        when(mediaJobRepository.save(any(MediaJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MediaJob updated = mediaJobService.markFailed(1L, "Processing error");

        assertThat(updated.getStatus()).isEqualTo(MediaJobStatus.FAILED);
        assertThat(updated.getCompletedAt()).isNotNull();
        assertThat(updated.getErrorMessage()).isEqualTo("Processing error");
    }

    @Test
    public void markProcessing_shouldThrowWhenJobNotFound() {
        when(mediaJobRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException ex =
                assertThrows(
                        NotFoundException.class,
                        () -> mediaJobService.markProcessing(99L));

        assertThat(ex.getCode())
            .isEqualTo(ErrorCode.MEDIA_JOB_NOT_FOUND);

        assertThat(ex.getMessage())
            .contains("Media job not found with id: 99");
    }

    @Test
    public void getMediaStatusForPost_shouldReturnMappedJobs() {
        MediaJob firstJob = mock(MediaJob.class);
        when(firstJob.getId()).thenReturn(1L);
        when(firstJob.getJobType()).thenReturn(MediaJobType.AI_TAGGING);
        when(firstJob.getStatus()).thenReturn(MediaJobStatus.COMPLETED);
        when(firstJob.getAttemptCount()).thenReturn(1);
        when(firstJob.getErrorMessage()).thenReturn(null);

        MediaJob secondJob = mock(MediaJob.class);
        when(secondJob.getId()).thenReturn(2L);
        when(secondJob.getJobType()).thenReturn(MediaJobType.AI_TAGGING);
        when(secondJob.getStatus()).thenReturn(MediaJobStatus.FAILED);
        when(secondJob.getAttemptCount()).thenReturn(2);
        when(secondJob.getErrorMessage()).thenReturn("AI provider timeout");

        when(mediaJobRepository.findByPostIdOrderByCreatedAtDesc(42L))
                .thenReturn(List.of(firstJob, secondJob));

        MediaStatusResponse result = mediaJobService.getMediaStatusForPost(42L);

        assertThat(result).isNotNull();
        assertThat(result.postId()).isEqualTo(42L);
        assertThat(result.jobs()).hasSize(2);

        MediaJobStatusResponse first = result.jobs().get(0);
        assertThat(first.id()).isEqualTo(1L);
        assertThat(first.jobType()).isEqualTo("AI_TAGGING");
        assertThat(first.status()).isEqualTo("COMPLETED");
        assertThat(first.attemptCount()).isEqualTo(1);
        assertThat(first.errorMessage()).isNull();

        MediaJobStatusResponse second = result.jobs().get(1);
        assertThat(second.id()).isEqualTo(2L);
        assertThat(second.jobType()).isEqualTo("AI_TAGGING");
        assertThat(second.status()).isEqualTo("FAILED");
        assertThat(second.attemptCount()).isEqualTo(2);
        assertThat(second.errorMessage()).isEqualTo("AI provider timeout");
    }

    @Test
    public void getMediaStatusForPost_shouldReturnEmptyJobsWhenNoJobsExist() {
        when(mediaJobRepository.findByPostIdOrderByCreatedAtDesc(99L))
                .thenReturn(List.of());

        MediaStatusResponse result = mediaJobService.getMediaStatusForPost(99L);

        assertThat(result).isNotNull();
        assertThat(result.postId()).isEqualTo(99L);
        assertThat(result.jobs()).isEmpty();
    }

}

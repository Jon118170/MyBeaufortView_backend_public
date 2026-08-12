package com.mybeaufortviewproject.mybeaufortview_backend.media;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.ErrorCode;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.NotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.media.dto.MediaJobStatusResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.media.dto.MediaStatusResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.post.Post;

@Service
@Transactional
public class MediaJobService {

    private final MediaJobRepository mediaJobRepository;

    public MediaJobService(MediaJobRepository mediaJobRepository) {
        this.mediaJobRepository = mediaJobRepository;
    }

    // Create a new media job for a given post and job type
    public MediaJob createJob(Post post, MediaJobType jobType) {
        MediaJob job = new MediaJob();
        job.setPost(post);
        job.setJobType(jobType);
        job.setStatus(MediaJobStatus.PENDING);
        job.setAttemptCount(0);

        return mediaJobRepository.save(job);
    }

    // Update job status to PROCESSING, increment attempt count, and set startedAt timestamp
    public MediaJob markProcessing(Long jobId) {
        MediaJob job = mediaJobRepository.findById(jobId)
                .orElseThrow(() -> mediaJobNotFound(jobId));

        job.setStatus(MediaJobStatus.PROCESSING);
        job.setStartedAt(Instant.now());
        job.setAttemptCount(job.getAttemptCount() + 1);
        job.setErrorMessage(null);

        return mediaJobRepository.save(job);
    }

    // Update job status to COMPLETED, set completedAt timestamp, and clear error message
    public MediaJob markCompleted(Long jobId) {
        MediaJob job = mediaJobRepository.findById(jobId)
                .orElseThrow(() -> mediaJobNotFound(jobId));

        job.setStatus(MediaJobStatus.COMPLETED);
        job.setCompletedAt(Instant.now());
        job.setErrorMessage(null);

        return mediaJobRepository.save(job);
    }

    // Update job status to FAILED, set completedAt timestamp, and record error message
    public MediaJob markFailed(Long jobId, String errorMessage) {
        MediaJob job = mediaJobRepository.findById(jobId)
                .orElseThrow(() -> mediaJobNotFound(jobId));

        job.setStatus(MediaJobStatus.FAILED);
        job.setErrorMessage(errorMessage);
        job.setCompletedAt(Instant.now());

        return mediaJobRepository.save(job);
    }

    // Retrieve all media jobs for a given post and return their statuses in a structured response
    @Transactional(readOnly = true)
    public MediaStatusResponse getMediaStatusForPost(Long postId) {
        List<MediaJob> jobs = mediaJobRepository.findByPostIdOrderByCreatedAtDesc(postId);

        List<MediaJobStatusResponse> jobResponses = jobs.stream()
                .map(job -> new MediaJobStatusResponse(
                        job.getId(),
                        job.getJobType().name(),
                        job.getStatus().name(),
                        job.getAttemptCount(),
                        job.getErrorMessage()
                ))
                .toList();

        return new MediaStatusResponse(postId, jobResponses);
    }

    // Helper method to create a NotFoundException for missing media jobs
    private NotFoundException mediaJobNotFound(Long jobId) {
        return new NotFoundException(
                ErrorCode.MEDIA_JOB_NOT_FOUND,
                "Media job not found with id: " + jobId
        );
    }
}

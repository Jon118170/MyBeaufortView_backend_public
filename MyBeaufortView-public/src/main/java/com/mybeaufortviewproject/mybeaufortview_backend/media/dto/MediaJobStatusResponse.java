package com.mybeaufortviewproject.mybeaufortview_backend.media.dto;

public record MediaJobStatusResponse (
        Long id,
        String jobType,
        String status,
        int attemptCount,
        String errorMessage
) {}

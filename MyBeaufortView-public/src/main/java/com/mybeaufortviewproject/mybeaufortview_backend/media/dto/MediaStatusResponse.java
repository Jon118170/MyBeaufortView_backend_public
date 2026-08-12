package com.mybeaufortviewproject.mybeaufortview_backend.media.dto;

import java.util.List;

public record MediaStatusResponse (
        Long postId,
        List<MediaJobStatusResponse> jobs
) {}

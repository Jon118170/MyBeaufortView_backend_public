package com.mybeaufortviewproject.mybeaufortview_backend.post.dto;

import java.time.Instant;
import java.util.List;

import com.mybeaufortviewproject.mybeaufortview_backend.location.dto.LocationResponse;

public record PostResponse (
        Long id,
        String description,
        String imageUrl,
        String thumbnailUrl,
        Instant createdAt,
        AuthorResponse author,
        LocationResponse location,
        long likeCount,
        boolean likedByMe,
        List<String> tags
) {}

package com.mybeaufortviewproject.mybeaufortview_backend.user.dto;

import java.time.Instant;

public record UserProfileResponse(
    Long id,
    String username,
    String name,
    String bio,
    String profileImageUrl,
    Instant createdAt,
    long postCount,
    long collectionCount,
    String sharePath
) {}

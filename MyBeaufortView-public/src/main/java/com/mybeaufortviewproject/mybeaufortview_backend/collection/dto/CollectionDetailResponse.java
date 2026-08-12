package com.mybeaufortviewproject.mybeaufortview_backend.collection.dto;

import java.time.Instant;

import com.mybeaufortviewproject.mybeaufortview_backend.common.dto.PageResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.AuthorResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.PostResponse;

public record CollectionDetailResponse (
    Long id,
    String title,
    AuthorResponse owner,
    Instant createdAt,
    long postCount,
    String coverImageUrl,
    PageResponse<PostResponse> posts
) {}

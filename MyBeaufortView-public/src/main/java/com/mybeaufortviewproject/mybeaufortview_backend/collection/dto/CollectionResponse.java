package com.mybeaufortviewproject.mybeaufortview_backend.collection.dto;

import com.mybeaufortviewproject.mybeaufortview_backend.collection.CollectionVisibility;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.AuthorResponse;

public record CollectionResponse (
    Long id,
    String title,
    AuthorResponse owner,
    long postCount,
    String coverImageUrl,
    CollectionVisibility visibility
) {}

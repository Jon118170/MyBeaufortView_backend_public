package com.mybeaufortviewproject.mybeaufortview_backend.post.dto;

public record AuthorResponse (
        Long id,
        String username,
        String name,
        String avatarUrl
) {}

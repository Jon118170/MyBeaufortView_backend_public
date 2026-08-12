package com.mybeaufortviewproject.mybeaufortview_backend.location.dto;

public record LocationResponse(
        Long id,
        String name,
        String slug,
        Double latitude,
        Double longitude,
        String description,
        long postCount
) {}

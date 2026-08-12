package com.mybeaufortviewproject.mybeaufortview_backend.collection.dto;

import com.mybeaufortviewproject.mybeaufortview_backend.collection.CollectionVisibility;

import jakarta.validation.constraints.NotBlank;

public record CollectionCreateRequest (
    @NotBlank(message = "Title is required")
    String title,
    CollectionVisibility visibility
) {}

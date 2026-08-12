package com.mybeaufortviewproject.mybeaufortview_backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest (
    @NotBlank(message = "Name is required")

    @Size(max = 100)
    String name,

    @Size(max = 1000)
    String bio,

    @Size(max = 512)
    String profileImageUrl

) {}

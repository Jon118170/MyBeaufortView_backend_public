package com.mybeaufortviewproject.mybeaufortview_backend.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PostUpdateRequest {

    @NotBlank
    private String description;

    @NotBlank
    private String imageUrl;

    @NotNull
    private Long locationId;

    public PostUpdateRequest() {
    }

    public PostUpdateRequest(String description, String imageUrl, Long locationId) {
        this.description = description;
        this.imageUrl = imageUrl;
        this.locationId = locationId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }
}

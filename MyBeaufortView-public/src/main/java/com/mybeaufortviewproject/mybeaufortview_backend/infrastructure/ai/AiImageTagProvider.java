package com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.ai;

public interface AiImageTagProvider {

    ImageTagResult generateTags(String imageUrl);
}

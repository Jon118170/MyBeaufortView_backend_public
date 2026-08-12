package com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.mybeaufortviewproject.mybeaufortview_backend.post.Post;

@Service
@ConditionalOnProperty(prefix = "aws.s3", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpThumbnailService implements ThumbnailService {

    @Override
    public void generateThumbnailForPost(Post post) {
        // No-op implementation for testing or when thumbnail generation is disabled
    }

}

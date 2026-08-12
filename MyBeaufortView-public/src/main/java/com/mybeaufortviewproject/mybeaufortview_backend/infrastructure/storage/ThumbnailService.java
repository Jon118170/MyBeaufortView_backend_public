package com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.storage;

import com.mybeaufortviewproject.mybeaufortview_backend.post.Post;

public interface ThumbnailService {
    public void generateThumbnailForPost(Post post);
}

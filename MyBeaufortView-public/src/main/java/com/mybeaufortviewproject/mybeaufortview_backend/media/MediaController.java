package com.mybeaufortviewproject.mybeaufortview_backend.media;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mybeaufortviewproject.mybeaufortview_backend.media.dto.MediaStatusResponse;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final MediaJobService mediaJobService;

    public MediaController(MediaJobService mediaJobService) {
        this.mediaJobService = mediaJobService;
    }

    @GetMapping("/{postId}/status")
    public MediaStatusResponse getMediaStatus(@PathVariable Long postId) {
        return mediaJobService.getMediaStatusForPost(postId);
    }

}

package com.mybeaufortviewproject.mybeaufortview_backend.like;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.UserPrincipal;

@RestController
@RequestMapping("/api/posts")
public class PostLikeController {

    private final LikeService likeService;

    public PostLikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    // POST /api/posts/{postId}/like - Like a post
    @PostMapping("/{postId}/like")
    public ResponseEntity<Void> like(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        likeService.likePost(postId, principal);
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/posts/{postId}/like - Unlike a post
    @DeleteMapping("/{postId}/like")
    public ResponseEntity<Void> unlike(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        likeService.unlikePost(postId, principal);
        return ResponseEntity.noContent().build();
    }

    // GET /api/posts/{postId}/liked - Whether current user liked the post
    @GetMapping("/{postId}/liked")
    public Boolean liked(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return likeService.hasLiked(postId, principal);
    }

    // GET /api/posts/{postId}/likes/count - Get like count for a post
    @GetMapping("/{postId}/likes/count")
    public Long likeCount(@PathVariable Long postId) {
        return likeService.countLikes(postId);
    }

}

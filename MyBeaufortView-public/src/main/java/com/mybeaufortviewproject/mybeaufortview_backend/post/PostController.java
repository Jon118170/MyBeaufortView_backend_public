package com.mybeaufortviewproject.mybeaufortview_backend.post;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mybeaufortviewproject.mybeaufortview_backend.common.dto.PageResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.UserPrincipal;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.PostCreateRequest;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.PostResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.PostUpdateRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PRIVILEGED_USER', 'ADMIN')")
    public ResponseEntity<PostResponse> addPost(
            @Valid @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PostResponse response = postService.createPost(request, principal.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public PostResponse getPostById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return postService.getPostResponseById(id, principal);
    }

    @GetMapping("/all")
    public List<PostResponse> getAllPosts(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return postService.getAllPosts(principal);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRIVILEGED_USER', 'ADMIN')")
    public PostResponse updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return postService.updatePost(id, request, principal);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRIVILEGED_USER', 'ADMIN')")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        postService.deletePost(id, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public PageResponse<PostResponse> getFeed(
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return PageResponse.from(
                postService.getFeed(pageable, principal)
        );
    }

    @GetMapping("/{id}/similar")
    public List<PostResponse> getSimilarPosts(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return postService.getSimilarPosts(id, principal);
    }
}

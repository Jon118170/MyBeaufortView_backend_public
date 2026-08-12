package com.mybeaufortviewproject.mybeaufortview_backend.collection;

import java.net.URI;

import org.springframework.data.domain.Pageable;
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

import com.mybeaufortviewproject.mybeaufortview_backend.collection.dto.CollectionCreateRequest;
import com.mybeaufortviewproject.mybeaufortview_backend.collection.dto.CollectionDetailResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.collection.dto.CollectionResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.collection.dto.CollectionUpdateRequest;
import com.mybeaufortviewproject.mybeaufortview_backend.common.dto.PageResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.UserPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    // Create a new collection
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CollectionResponse> createCollection(
            @Valid @RequestBody CollectionCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
            CollectionResponse created = collectionService.createCollection(request, principal);
            return ResponseEntity
                    .created(URI.create("/api/collections/" + created.id()))
                    .body(created);
    }

    // Get all collections with pagination
    @GetMapping
    public PageResponse<CollectionResponse> getCollections(Pageable pageable) {
        return PageResponse.from(collectionService.getCollections(pageable));
    }

    // Get a specific collection by ID
    @GetMapping("/{collectionId}")
    public CollectionDetailResponse getCollection(
            @PathVariable Long collectionId,
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return collectionService.getCollection(collectionId, pageable, principal);
    }

    // Update a collection
    @PutMapping("/{collectionId}")
    @PreAuthorize("isAuthenticated()")
    public CollectionResponse updateCollection(
            @PathVariable Long collectionId,
            @Valid @RequestBody CollectionUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return collectionService.updateCollection(collectionId, request, principal);
    }

    // Delete a collection
    @DeleteMapping("/{collectionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteCollection(
            @PathVariable Long collectionId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        collectionService.deleteCollection(collectionId, principal);
        return ResponseEntity.noContent().build();
    }

    // Add a post to a collection
    @PostMapping("/{collectionId}/posts/{postId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> addPostToCollection(
            @PathVariable Long collectionId,
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        collectionService.addPostToCollection(collectionId, postId, principal);
        return ResponseEntity.noContent().build();
    }

    // Remove a post from a collection
    @DeleteMapping("/{collectionId}/posts/{postId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removePostFromCollection(
            @PathVariable Long collectionId,
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        collectionService.removePostFromCollection(collectionId, postId, principal);
        return ResponseEntity.noContent().build();
    }
}

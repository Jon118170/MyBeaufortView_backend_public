package com.mybeaufortviewproject.mybeaufortview_backend.collection;

import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mybeaufortviewproject.mybeaufortview_backend.collection.dto.CollectionResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.common.dto.PageResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.UserPrincipal;

@RestController
@RequestMapping("/api/users")
public class UserCollectionController {

    private final CollectionService collectionService;

    public UserCollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    // GET /api/users/{userId}/collections - Get paginated collections for a user
    @GetMapping("/{userId}/collections")
    public PageResponse<CollectionResponse> getCollectionsByUser(
            @PathVariable Long userId,
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return PageResponse.from(
                collectionService.getCollectionsByUserId(userId, pageable, principal)
        );
    }

}

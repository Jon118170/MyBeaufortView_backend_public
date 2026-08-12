package com.mybeaufortviewproject.mybeaufortview_backend.location;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mybeaufortviewproject.mybeaufortview_backend.common.dto.PageResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.UserPrincipal;
import com.mybeaufortviewproject.mybeaufortview_backend.location.dto.LocationResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostService;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.PostResponse;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;
    private final PostService postService;

    public LocationController(LocationService locationService, PostService postService) {
        this.locationService = locationService;
        this.postService = postService;
    }

    // Get all locations, sorted by name
    @GetMapping
    public List<LocationResponse> getAllLocations() {
        return locationService.getAllLocations();
    }

    // Get location details by slug
    @GetMapping("/{slug}")
    public LocationResponse getLocationBySlug(@PathVariable String slug) {
        return locationService.getLocationBySlug(slug);
    }

    // Get posts for a location by slug with pagination
    @GetMapping("/{slug}/posts")
    public PageResponse<PostResponse> getPostsByLocation(
            @PathVariable String slug,
            @PageableDefault(
                    size = 12,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return PageResponse.from(
                postService.getPostsByLocationSlug(slug, pageable, principal)
        );
    }
}

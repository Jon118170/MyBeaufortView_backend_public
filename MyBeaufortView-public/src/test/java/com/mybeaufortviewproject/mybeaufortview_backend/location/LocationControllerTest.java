package com.mybeaufortviewproject.mybeaufortview_backend.location;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.LocationNotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.location.dto.LocationResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostService;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.AuthorResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.PostResponse;

@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WebMvcTest(LocationController.class)
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocationService locationService;

    @MockitoBean
    private PostService postService;

    @Test
    public void getAllLocations_returnsOrderedLocationList() throws Exception {
         LocationResponse huntingIsland = new LocationResponse(
                 1L,
                 "Hunting Island",
                 "hunting-island",
                 32.3738,
                 -80.4512,
                 "Barrier island known for lighthouse views and marsh sunsets.",
                 14L
         );

         LocationResponse waterfrontPark = new LocationResponse(
                 2L,
                 "Waterfront Park",
                 "waterfront-park",
                 32.4310,
                 -80.6690,
                 "Popular waterfront destination in Beaufort.",
                 7L
         );

        when(locationService.getAllLocations())
                .thenReturn(List.of(huntingIsland, waterfrontPark));

        mockMvc.perform(get("/api/locations").accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Hunting Island"))
                .andExpect(jsonPath("$[0].slug").value("hunting-island"))
                .andExpect(jsonPath("$[0].latitude").value(32.3738))
                .andExpect(jsonPath("$[0].longitude").value(-80.4512))
                .andExpect(jsonPath("$[0].description").value("Barrier island known for lighthouse views and marsh sunsets."))
                .andExpect(jsonPath("$[0].postCount").value(14))

                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Waterfront Park"))
                .andExpect(jsonPath("$[1].slug").value("waterfront-park"))
                .andExpect(jsonPath("$[1].latitude").value(32.4310))
                .andExpect(jsonPath("$[1].longitude").value(-80.6690))
                .andExpect(jsonPath("$[1].description").value("Popular waterfront destination in Beaufort."))
                .andExpect(jsonPath("$[1].postCount").value(7));
    }

    @Test
    public void getAllLocations_returnsEmptyListWhenNoLocationsExist() throws Exception {
        when(locationService.getAllLocations()).thenReturn(List.of());

        mockMvc.perform(get("/api/locations").accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void getLocationBySlug_returnsLocation() throws Exception {
         LocationResponse huntingIsland = new LocationResponse(
                 1L,
                 "Hunting Island",
                 "hunting-island",
                 32.3738,
                 -80.4512,
                 "Barrier island known for lighthouse views and marsh sunsets.",
                 14L
         );

        when(locationService.getLocationBySlug("hunting-island"))
            .thenReturn(huntingIsland);

        mockMvc.perform(get("/api/locations/hunting-island").accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Hunting Island"))
                .andExpect(jsonPath("$.slug").value("hunting-island"))
                .andExpect(jsonPath("$.latitude").value(32.3738))
                .andExpect(jsonPath("$.longitude").value(-80.4512))
                .andExpect(jsonPath("$.description").value("Barrier island known for lighthouse views and marsh sunsets."))
                .andExpect(jsonPath("$.postCount").value(14));
    }

    @Test
    public void getPostsByLocation_returnsPagedPosts() throws Exception {
        PostResponse post = new PostResponse(
                99L,
                "Sunset on the marsh",
                "https://img/sunset.jpg",
                "https://img/sunset-thumb.jpg",
                Instant.parse("2024-01-01T12:00:00Z"),
                new AuthorResponse(11L, "photog2", "John Smith", null),
                new LocationResponse(
                        1L,
                        "Hunting Island",
                        "hunting-island",
                        32.3738,
                        -80.4512,
                        "Barrier island known for lighthouse views and marsh sunsets.",
                        0L
                ),
                12L,
                false,
                List.of("sunset", "marsh", "water")
    );

        Page<PostResponse> postsPage =
                new PageImpl<>(List.of(post), PageRequest.of(0,  12), 1);

        when(postService.getPostsByLocationSlug(
                eq("hunting-island"),
                any(Pageable.class),
                isNull()
        )).thenReturn(postsPage);

        mockMvc.perform(get("/api/locations/hunting-island/posts?page=0&size=12")
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(99))
                .andExpect(jsonPath("$.items[0].description").value("Sunset on the marsh"))
                .andExpect(jsonPath("$.items[0].location.id").value(1))
                .andExpect(jsonPath("$.items[0].location.slug").value("hunting-island"))
                .andExpect(jsonPath("$.items[0].location.description")
                        .value("Barrier island known for lighthouse views and marsh sunsets."))
                .andExpect(jsonPath("$.items[0].tags[0]").value("sunset"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    public void getPostsByLocation_returns404WhenSlugDoesNotExist() throws Exception {
        when(postService.getPostsByLocationSlug(
                eq("not-real-place"),
                any(Pageable.class),
                isNull()
        )).thenThrow(new LocationNotFoundException("Location not found with slug: nonexistent-slug"));

        mockMvc.perform(get("/api/locations/not-real-place/posts")
                    .accept(APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

}

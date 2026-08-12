package com.mybeaufortviewproject.mybeaufortview_backend.collection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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

import com.mybeaufortviewproject.mybeaufortview_backend.collection.CollectionVisibility;
import com.mybeaufortviewproject.mybeaufortview_backend.collection.dto.CollectionDetailResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.collection.dto.CollectionResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.common.dto.PageResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.location.dto.LocationResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.AuthorResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.PostResponse;

@WebMvcTest(CollectionController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public class CollectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CollectionService collectionService;

    @Test
    public void getCollections_returnsPage() throws Exception {

        // Prepare a sample CollectionResponse
        AuthorResponse owner = new AuthorResponse(10L, "photog1", "Jane Doe", null);
        CollectionResponse response =
                new CollectionResponse(1L, "Lowcountry Stories!", owner, 5L, "https://img/cover.jpg", CollectionVisibility.PUBLIC);

        // Create a page with the single response
        Page<CollectionResponse> page =
                new PageImpl<>(List.of(response), PageRequest.of(0, 12), 1);

        // Mock the service to return the page
        when(collectionService.getCollections(any(Pageable.class))).thenReturn(page);

        // Perform the GET request and verify the response
        mockMvc.perform(get("/api/collections?page=0&size=12"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(1))
        .andExpect(jsonPath("$.items[0].title").value("Lowcountry Stories!"))
        .andExpect(jsonPath("$.items[0].owner.username").value("photog1"))
        .andExpect(jsonPath("$.items[0].postCount").value(5))
        .andExpect(jsonPath("$.items[0].coverImageUrl").value("https://img/cover.jpg"))
        .andExpect(jsonPath("$.totalItems").value(1));

    }

    @Test
    public void getCollection_returnsDetailWithPostsPage() throws Exception {

        // Prepare sample data
        AuthorResponse owner = new AuthorResponse(10L, "photog1", "Jane Doe", null);
        AuthorResponse author = new AuthorResponse(11L, "photog2", "John Smith", null);

        LocationResponse location = new LocationResponse(
                1L,
                "Hunting Island",
                "hunting-island",
                32.3738,
                -80.4512,
                "Barrier island known for lighthouse views and marsh sunsets.",
                0L
        );
        // Sample PostResponse
        PostResponse post = new PostResponse(
                99L,
                "Sunset on the marsh",
                "https://img/sunset.jpg",
                "https://img/sunset-thumb.jpg",
                Instant.parse("2024-01-01T12:00:00Z"),
                author,
                location,
                12L,
                false,
                List.of("sunset", "marsh", "water")
        );

        // Create a page with the single post
        PageResponse<PostResponse> postsPage = PageResponse.from(
                new PageImpl<>(List.of(post), PageRequest.of(0, 12), 1)
        );

        // Prepare CollectionDetailResponse with the posts page
        CollectionDetailResponse detail = new CollectionDetailResponse(
                1L,
                "Lowcountry Stories!",
                owner,
                Instant.parse("2024-01-15T10:00:00Z"),
                5L,
                "https://img/cover.jpg",
                postsPage
        );

        // Mock the service to return the detail
        when(collectionService.getCollection(eq(1L), any(Pageable.class), eq(null)))
            .thenReturn(detail);


        // Perform the GET request and verify the response
        mockMvc.perform(get("/api/collections/1?page=0&size=12&sort=addedAt,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Lowcountry Stories!"))
            .andExpect(jsonPath("$.owner.username").value("photog1"))
            .andExpect(jsonPath("$.posts.items[0].id").value(99))
            .andExpect(jsonPath("$.posts.items[0].likeCount").value(12))
            .andExpect(jsonPath("$.posts.totalItems").value(1))
            .andExpect(jsonPath("$.createdAt").value("2024-01-15T10:00:00Z"))
            .andExpect(jsonPath("$.posts.items[0].likedByMe").value(false))
            .andExpect(jsonPath("$.posts.items[0].location.description").value("Barrier island known for lighthouse views and marsh sunsets."))
            .andExpect(jsonPath("$.posts.items[0].location.postCount").value(0))
            .andExpect(jsonPath("$.posts.items[0].tags[0]").value("sunset"))
            .andExpect(jsonPath("$.posts.items[0].tags[1]").value("marsh"))
            .andExpect(jsonPath("$.posts.items[0].tags[2]").value("water"));

    }
}

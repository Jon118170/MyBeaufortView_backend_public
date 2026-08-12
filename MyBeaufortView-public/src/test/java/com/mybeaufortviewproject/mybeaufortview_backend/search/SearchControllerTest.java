package com.mybeaufortviewproject.mybeaufortview_backend.search;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mybeaufortviewproject.mybeaufortview_backend.location.dto.LocationResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostService;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.AuthorResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.PostResponse;

@WebMvcTest(SearchController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    @Test
    public void searchPosts_returnsMatchingResults() throws Exception {
        PostResponse post = new PostResponse(
                1L,
                "Golden sunset over the marsh",
                "https://example.com/image.jpg",
                "https://example.com/thumbnail.jpg",
                Instant.parse("2024-01-01T18:30:00Z"),
                new AuthorResponse(10L, "photog1", "Jane Doe", null),
                new LocationResponse(
                        1L,
                        "Hunting Island",
                        "hunting-island",
                        32.3738,
                        -80.4512,
                        "Barrier island known for lighthouse views and marsh sunsets.",
                        0L
                ),
                5L,
                false,
                List.of("sunset", "marsh", "nature")
        );

        when(postService.searchPostResponses("sunset", "desc"))
            .thenReturn(List.of(post));

        mockMvc.perform(get("/api/search")
                    .param("q", "sunset")
                    .param("sort", "desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].description").value("Golden sunset over the marsh"))
            .andExpect(jsonPath("$[0].author.username").value("photog1"))
            .andExpect(jsonPath("$[0].location.slug").value("hunting-island"))
            .andExpect(jsonPath("$[0].tags[0]").value("sunset"));
    }

    @Test
    public void searchPosts_returnsResultsWhenKeywordMathesTagsOnly() throws Exception {
        PostResponse post = new PostResponse(
                2L,
                "Golden hour over the marsh",
                "https://example.com/image2.jpg",
                "https://example.com/thumbnail2.jpg",
                Instant.parse("2024-01-02T18:30:00Z"),
                new AuthorResponse(1L, "photog1", "John Smith", null),
                new LocationResponse(
                        1L,
                        "Hunting Island",
                        "hunting-island",
                        32.3738,
                        -80.4512,
                        "Barrier island known for lighthouse views and marsh sunsets.",
                        0L
                ),
                5L,
                false,
                List.of("sunset", "marsh", "nature")
        );

        when (postService.searchPostResponses("sunset", "desc"))
            .thenReturn(List.of(post));

        mockMvc.perform(get("/api/search")
                .param("q", "sunset")
                .param("sort", "desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].description").value("Golden hour over the marsh"))
            .andExpect(jsonPath("$[0].tags[0]").value("sunset"));
    }
}

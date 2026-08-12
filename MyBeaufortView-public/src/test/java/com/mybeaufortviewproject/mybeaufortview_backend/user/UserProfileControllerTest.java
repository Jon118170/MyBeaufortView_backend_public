package com.mybeaufortviewproject.mybeaufortview_backend.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mybeaufortviewproject.mybeaufortview_backend.location.dto.LocationResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.AuthorResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.PostResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.user.dto.UserProfileResponse;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserService userService;

    @Test
    public void getUserProfile_returnsResponse() throws Exception {
        Long userId = 1L;

        UserProfileResponse response = new UserProfileResponse(
                1L,
                "photog1",
                "Jane Doe",
                null,
                null,
                null,
                12L,
                3L,
                "/users/1"
        );

        when(userProfileService.getProfile(userId)).thenReturn(response);

        mockMvc.perform(get("/api/users/{userId}/profile", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.username").value("photog1"))
            .andExpect(jsonPath("$.name").value("Jane Doe"))
            .andExpect(jsonPath("$.postCount").value(12))
            .andExpect(jsonPath("$.collectionCount").value(3))
            .andExpect(jsonPath("$.sharePath").value("/users/1"));
    }

    @Test
    public void getUserPosts_returnsPage() throws Exception {
        Long userId = 1L;

        LocationResponse location = new LocationResponse(
                1L,
                "Hunting Island",
                "hunting-island",
                32.3738,
                -80.4512,
                "Barrier island known for lighthouse views and marsh sunsets.",
                0L
        );
        PostResponse post = new PostResponse(
                10L,
                "Sunset on the water",
                "https://example.com/photo.jpg",
                "https://example.com/photo-thumb.jpg",
                Instant.parse("2024-01-01T12:00:00Z"),
                new AuthorResponse(1L, "photog1", "Jane Doe", null),
                location,
                5L,
                false,
                List.of("sunset", "water", "nature")
        );

        Page<PostResponse> page = new PageImpl<>(
                List.of(post),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                1
        );

        when(userProfileService.getUserPosts(eq(userId),
                any(Pageable.class),
                isNull()))
                .thenReturn(page);

        mockMvc.perform(get("/api/users/{userId}/posts?page=0&size=20&sort=createdAt,desc", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(10))
                .andExpect(jsonPath("$.items[0].likeCount").value(5))
                .andExpect(jsonPath("$.items[0].likedByMe").value(false))
                .andExpect(jsonPath("$.items[0].author.id").value(1))
                .andExpect(jsonPath("$.items[0].author.username").value("photog1"))
                .andExpect(jsonPath("$.items[0].location.slug").value("hunting-island"))
                .andExpect(jsonPath("$.items[0].location.description")
                        .value("Barrier island known for lighthouse views and marsh sunsets."))
                .andExpect(jsonPath("$.items[0].tags[0]").value("sunset"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }
}

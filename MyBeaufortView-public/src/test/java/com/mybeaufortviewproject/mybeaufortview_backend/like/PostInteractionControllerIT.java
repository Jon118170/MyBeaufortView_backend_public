package com.mybeaufortviewproject.mybeaufortview_backend.like;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.JwtUtil;
import com.mybeaufortviewproject.mybeaufortview_backend.location.Location;
import com.mybeaufortviewproject.mybeaufortview_backend.location.LocationRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.post.Post;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.user.Role;
import com.mybeaufortviewproject.mybeaufortview_backend.user.User;
import com.mybeaufortviewproject.mybeaufortview_backend.user.UserRepository;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@Transactional
public class PostInteractionControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;

    @Autowired private UserRepository userRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private LocationRepository locationRepository;

    private User user;
    private Post post;
    private Location location;
    private String token;

    @BeforeEach
    void setup() {
        user = new User();
        user.setName("Jane Doe");
        user.setUsername("janedoe");
        user.setEmail("jane@example.com");
        user.setPassword("password456");
        user.setRole(Role.PRIVILEGED_USER);

        user = userRepository.save(user);

        location = new Location();
        location.setName("Hunting Island");
        location.setSlug("hunting-island-" + user.getId());
        location.setLatitude(32.3738);
        location.setLongitude(-80.4512);
        location.setDescription("Test location for interaction tests");

        location = locationRepository.save(location);

        post = new Post();
        post.setDescription("Sample Post");
        post.setImageUrl("http://example.com/image.jpg");
        post.setUser(user);
        post.setLocation(location);

        post = postRepository.save(post);

        token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
    }

    @Test
    void like_returns204() throws Exception {

        // First like the post
        mockMvc.perform(post("/api/posts/{postId}/like", post.getId())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
    }

    @Test
    void unlike_returns204() throws Exception {

        // First like the post
        mockMvc.perform(post("/api/posts/{postId}/like", post.getId())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        // Then unlike the post
        mockMvc.perform(delete("/api/posts/{postId}/like", post.getId())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        // Trying to unlike again should still return 204
        mockMvc.perform(delete("/api/posts/{postId}/like", post.getId())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
    }

    @Test
    void like_trueAfterLike_falseAfterUnlike() throws Exception {

        // Initially should be not liked
        mockMvc.perform(get("/api/posts/{postId}/liked", post.getId())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().string("false"));

        // Like the post
        mockMvc.perform(post("/api/posts/{postId}/like", post.getId())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        // Should be liked now
        mockMvc.perform(get("/api/posts/{postId}/liked", post.getId())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().string("true"));

        // Unlike the post
        mockMvc.perform(delete("/api/posts/{postId}/like", post.getId())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());


        // Should be not liked again
        mockMvc.perform(get("/api/posts/{postId}/liked", post.getId())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().string("false"));
    }

}

package com.mybeaufortviewproject.mybeaufortview_backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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
@ExtendWith(MockitoExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@Transactional
public class PostSecurityIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User admin;
    private User owner;
    private User otherPriv;

    private String adminToken;
    private String ownerToken;
    private String otherPrivToken;

    private Post ownersPost;

    private Location testLocation;

    @BeforeEach
    void setup() {
        // Mockito.doNothing().when(amazonS3).putObject(Mockito.anyString(),
        // Mockito.anyString(), Mockito.anyString());

        admin = SecurityTestUtils.seedUser(userRepository, passwordEncoder, "admin@example.com", Role.ADMIN);
        owner = SecurityTestUtils.seedUser(userRepository, passwordEncoder, "owner@example.com", Role.PRIVILEGED_USER);
        otherPriv = SecurityTestUtils.seedUser(userRepository, passwordEncoder, "other@example.com",
                Role.PRIVILEGED_USER);

        adminToken = SecurityTestUtils.tokenFor(admin, jwtUtil);
        ownerToken = SecurityTestUtils.tokenFor(owner, jwtUtil);
        otherPrivToken = SecurityTestUtils.tokenFor(otherPriv, jwtUtil);

        testLocation = new Location();
        testLocation.setName("Hunting Island");
        testLocation.setSlug("hunting-island" + System.nanoTime());
        testLocation.setLatitude(32.3738);
        testLocation.setLongitude(-80.4512);
        testLocation.setDescription("Test location for post security tests");
        testLocation = locationRepository.saveAndFlush(testLocation);

        ownersPost = new Post();
        ownersPost.setUser(owner);
        ownersPost.setDescription("Owner's Post");
        ownersPost.setImageUrl("http://example.com/image.jpg");
        ownersPost.setCreatedAt(Instant.now());
        ownersPost.setLocation(testLocation);
        ownersPost = postRepository.save(ownersPost);
    }

    // ---------------
    // Public GET endpoints
    // ---------------

    @Test
    void noToken_getAllPosts_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/posts")).andExpect(status().isOk());
    }

    @Test
    void noToken_getPostById_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/posts/{id}", ownersPost.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ownersPost.getId()));
    }

    // --------------
    // POST /api/posts (privileged/admin users only)
    // --------------

    @Test
    void noToken_createPost_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"Hello\",\"imageUrl\":\"\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void privileged_createPost_shouldReturn201() throws Exception {
        String payload = String.format("""
                { "description": "New Post",
                  "imageUrl": "http://example.com/pic1.jpg",
                  "locationId": %d }
                """, testLocation.getId());

        mockMvc.perform(post("/api/posts").with(SecurityTestUtils.bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("New Post"))
                .andExpect(jsonPath("$.author.id").value(owner.getId()))
                .andExpect(jsonPath("$.location.id").value(testLocation.getId()));
    }

    // --------------
    // PUT /api/posts/{id} (privileged/admin users only)
    // --------------

    @Test
    void otherPriv_updateOwnersPost_shouldReturn403() throws Exception {
        String payload = String.format("""
                { "description": "Hacked",
                  "imageUrl": "http://example.com/Hacked.jpg",
                  "locationId": %d }
                """, testLocation.getId());

        mockMvc.perform(put("/api/posts/{id}", ownersPost.getId()).with(SecurityTestUtils.bearer(otherPrivToken))
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
    }

    @Test
    void owner_updateOwnPost_shouldReturn200() throws Exception {
        String payload = String.format("""
                     { "description": "Updated",
                       "imageUrl": "http://example.com/updated.jpg",
                       "locationId": %d }
                """, testLocation.getId());

        mockMvc.perform(put("/api/posts/{id}", ownersPost.getId()).with(SecurityTestUtils.bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated"))
                .andExpect(jsonPath("$.location.id").value(testLocation.getId()));
    }

    @Test
    void admin_updateAnyPost_shouldReturn200() throws Exception {
        String payload = String.format("""
                { "description": "Updated",
                  "imageUrl": "http://example.com/updated.jpg",
                  "locationId": %d }
                """, testLocation.getId());

        mockMvc.perform(put("/api/posts/{id}", ownersPost.getId()).with(SecurityTestUtils.bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated"))
                .andExpect(jsonPath("$.location.id").value(testLocation.getId()));
    }

    // --------------
    // DELETE /api/posts/{id} (privileged/admin users only)
    // --------------

    @Test
    void otherPriv_deleteOwnersPost_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/posts/{id}", ownersPost.getId()).with(SecurityTestUtils.bearer(otherPrivToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void owner_deleteOwnPost_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/posts/{id}", ownersPost.getId()).with(SecurityTestUtils.bearer(ownerToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void admin_deleteAnyPost_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/posts/{id}", ownersPost.getId()).with(SecurityTestUtils.bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    // --------------
    // Invalid token path
    // --------------

    @Test
    void invalidToken_updatePost_shouldReturn401() throws Exception {
        String payload = String.format("""
                { "description": "Admin updated",
                  "imageUrl": "http://example.com/admin.jpg",
                  "locationId": %d }
                """, testLocation.getId());

        mockMvc.perform(put("/api/posts/{id}", ownersPost.getId()).header("Authorization", "Bearer not-a-jwt")
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isUnauthorized());
    }

}

package com.mybeaufortviewproject.mybeaufortview_backend.post;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.JwtUtil;
import com.mybeaufortviewproject.mybeaufortview_backend.like.Like;
import com.mybeaufortviewproject.mybeaufortview_backend.like.LikeRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.location.Location;
import com.mybeaufortviewproject.mybeaufortview_backend.location.LocationRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.security.SecurityTestUtils;
import com.mybeaufortviewproject.mybeaufortview_backend.user.Role;
import com.mybeaufortviewproject.mybeaufortview_backend.user.User;
import com.mybeaufortviewproject.mybeaufortview_backend.user.UserRepository;

import io.jsonwebtoken.Claims;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@Transactional
public class PostControllerIT {

    private static final Logger logger = LoggerFactory.getLogger(PostControllerIT.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private LocationRepository locationRepository;

    private Location testLocation;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PostTagRepository postTagRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private Long testPostId;

    private User owner;

    private String ownerToken;

    private Post post;

    private User viewer;

    private String viewerToken;

    @BeforeEach
    public void setup() throws Exception {

        likeRepository.deleteAll();
        postTagRepository.deleteAll();
        postRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();

        owner = SecurityTestUtils.seedUser(userRepository, passwordEncoder,
                "owner@example.com", Role.PRIVILEGED_USER);
        ownerToken = SecurityTestUtils.tokenFor(owner, jwtUtil);

        viewer = SecurityTestUtils.seedUser(userRepository, passwordEncoder,
                "viewer@example.com", Role.PRIVILEGED_USER);
        viewerToken = SecurityTestUtils.tokenFor(viewer, jwtUtil);

        assertNotNull(ownerToken);
        assertFalse(ownerToken.isBlank());
        assertNotNull(viewerToken);
        assertFalse(viewerToken.isBlank());

        testLocation = new Location();
        testLocation.setName("Hunting Island");
        testLocation.setSlug("hunting-island");
        testLocation.setLatitude(32.3738);
        testLocation.setLongitude(-80.4512);
        testLocation = locationRepository.saveAndFlush(testLocation);

        // Mock setup a Post
        Post p = new Post();

        p.setUser(owner);
        p.setDescription("Test Post Description");
        p.setImageUrl("http://example.com/image.jpg");
        p.setCreatedAt(Instant.now());
        p.setLocation(testLocation);

        post = postRepository.saveAndFlush(p);
        testPostId = post.getId();

        // Prepare JSON for creating a new Post
        logger.info("Created Post with ID: {} ", testPostId);
    }

    @Test
    public void testGetPostById() throws Exception {
        logger.info("Starting testGetPostById with Post ID: {}",  testPostId);

        mockMvc.perform(get("/api/posts/{id}", testPostId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testPostId));

        logger.info("testGetPostById completed successfully for Post ID: {} ", testPostId);
    }

    @Test
    public void testGetFeed() throws Exception {
        logger.info("Testing getAllPosts");

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").exists());

        logger.info("testGetAllPosts completed successfully");
    }

    @Test
    public void testCreatePost() throws Exception {

        // Prepare JSON for creating a new Post
        String newPostJson = String.format("""
                {
                "description": "New Test Post Description",
                "imageUrl": "http://example.com/newimage.jpg",
                "locationId": %d
                }
                """, testLocation.getId());

        logger.info("Testing createPost with payload: {}", newPostJson);

        mockMvc.perform(post("/api/posts")
                .with(SecurityTestUtils.bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(newPostJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("New Test Post Description"))
                .andExpect(jsonPath("$.imageUrl").value("http://example.com/newimage.jpg"))
                .andExpect(jsonPath("$.author.id").value(owner.getId()))
                .andExpect(jsonPath("$.location.id").value(testLocation.getId()))
                .andExpect(jsonPath("$.location.name").value("Hunting Island"));

        logger.info("testCreatePost completed successfully");
    }

    @Test
    public void testUpdatePost() throws Exception {
        // Prepare JSON for updating the Post
        String updatedPostJson = String.format("""
            {
             "description": "Updated Test Post Description",
             "imageUrl": "http://example.com/updatedimage.jpg",
             "locationId": %d
             }
            """, testLocation.getId());

        logger.info("Testing updatePost with ID: {} and payload: {}", testPostId, updatedPostJson);

        mockMvc.perform(
                put("/api/posts/{id}", testPostId)
                .with(SecurityTestUtils.bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedPostJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated Test Post Description"))
                .andExpect(jsonPath("$.imageUrl").value("http://example.com/updatedimage.jpg"))
                .andExpect(jsonPath("$.location.id").value(testLocation.getId()));

        logger.info("testUpdatePost completed successfully for Post ID: {}", testPostId);

    }

    @Test
    public void testDeletePost() throws Exception {
        logger.info("Testing deletePost with ID: {}", testPostId);

        mockMvc.perform(delete("/api/posts/{id}", testPostId)
                .with(SecurityTestUtils.bearer(ownerToken)))
                .andExpect(status().isNoContent());

        logger.info("testDeletePost completed successfully for Post ID: {}", testPostId);
    }

    @Test
    public void getFeed_public_returnsPage_andLikedByMeFalse() throws Exception {

        mockMvc.perform(get("/api/posts")
                .header("Authorization", "Bearer " + viewerToken)
                .param("page", "0")
                .param("size", "20")
                .param("sort", "createdAt,desc"))
            .andExpect(status().isOk())
            // Page shape => content array
            .andExpect(jsonPath("$.items[0].id").value(post.getId()))
            .andExpect(jsonPath("$.items[0].description").value("Test Post Description"))
            .andExpect(jsonPath("$.items[0].imageUrl").value("http://example.com/image.jpg"))
            .andExpect(jsonPath("$.items[0].likeCount").value(0))
            .andExpect(jsonPath("$.items[0].likedByMe").value(false))
            .andExpect(jsonPath("$.items[0].author.username").value(owner.getUsername()))
            .andExpect(jsonPath("$.items[0].author.name").value(owner.getName()));

    }

    @Test
    public void getFeed_aauthenticated_viewerLiked_seesLikedByMeTrue() throws Exception {

        // Fetch the persisted post
        Post persistedPost = postRepository.findById(testPostId)
                .orElseThrow();

        // Create a Like by viewer on the post
        Like like = new Like();
        like.setUser(viewer);
        like.setPost(persistedPost);
        likeRepository.saveAndFlush(like);

        assertTrue(likeRepository.existsByUser_IdAndPost_Id(viewer.getId(), post.getId()));

        // Validate token and claims
        Claims claims = jwtUtil.validateToken(viewerToken);
        assertEquals(viewer.getId(), claims.get("id", Long.class));
        assertEquals(viewer.getRole().name(), claims.get("role", String.class));
        assertEquals(viewer.getEmail(), claims.getSubject());

        // Call feed as viewer -> likedByMe = true, likeCount = 1
        mockMvc.perform(get("/api/posts")
                .header("Authorization", "Bearer " + viewerToken)
                .param("page", "0")
                .param("size", "20")
                .param("sort", "createdAt,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value(testPostId))
            .andExpect(jsonPath("$.items[0].likeCount").value(1))
            .andExpect(jsonPath("$.items[0].likedByMe").value(true));
    }

    @Test
    public void getFeed_authenticated_ownerDidNotLike_seesLikedByMeFalse() throws Exception {

        // Fetch the persisted post
                Post persistedPost = postRepository.findById(testPostId)
                        .orElseThrow();

                // Create a Like by viewer on the post
                Like like = new Like();
                like.setUser(viewer);
                like.setPost(persistedPost);
                likeRepository.saveAndFlush(like);

                assertTrue(likeRepository.existsByUser_IdAndPost_Id(viewer.getId(), post.getId()));

                // Validate token and claims
                Claims claims = jwtUtil.validateToken(viewerToken);
                assertEquals(viewer.getId(), claims.get("id", Long.class));
                assertEquals(viewer.getRole().name(), claims.get("role", String.class));
                assertEquals(viewer.getEmail(), claims.getSubject());

                // Call feed as viewer -> likedByMe = true, likeCount = 1
                mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "createdAt,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[0].id").value(testPostId))
                    .andExpect(jsonPath("$.items[0].likeCount").value(1))
                    .andExpect(jsonPath("$.items[0].likedByMe").value(false));
    }

    @Test
    public void privileged_createPostWithS3ImageUrl_shouldReturn201() throws Exception {
        String payload = String.format("""
                {
                "description": "Post with S3 Image",
                "imageUrl": "https://my-bucket.s3.us-east-1.amazonaws.com/uploads/unique-file-id.jpg",
                "locationId": %d
                }
                """, testLocation.getId());

        mockMvc.perform(post("/api/posts")
                .with(SecurityTestUtils.bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.description").value("Post with S3 Image"))
            .andExpect(jsonPath("$.imageUrl").value("https://my-bucket.s3.us-east-1.amazonaws.com/uploads/unique-file-id.jpg"))
            .andExpect(jsonPath("$.author.id").value(owner.getId()))
            .andExpect(jsonPath("$.location.id").value(testLocation.getId()));
    }

    @Test
    public void getSimilarPosts_returnsMatchingPosts() throws Exception {
        Post similarPost = new Post();
        similarPost.setUser(viewer);
        similarPost.setDescription("Waterfront sunset");
        similarPost.setImageUrl("https://example.com/similar.jpg");
        similarPost.setCreatedAt(Instant.parse("2024-01-02T18:30:00Z"));
        similarPost.setLocation(testLocation);
        similarPost = postRepository.saveAndFlush(similarPost);

        Post unrelatedPost = new Post();
        unrelatedPost.setUser(viewer);
        unrelatedPost.setDescription("Forest trail");
        unrelatedPost.setImageUrl("https://example.com/unrelated.jpg");
        unrelatedPost.setCreatedAt(Instant.parse("2024-01-03T18:30:00Z"));
        unrelatedPost.setLocation(testLocation);
        unrelatedPost = postRepository.saveAndFlush(unrelatedPost);

        postTagRepository.saveAll(List.of(
                new PostTag(null, post, "sunset"),
                new PostTag(null, post, "marsh"),

                new PostTag(null, similarPost, "sunset"),
                new PostTag(null, similarPost, "water"),

                new PostTag(null, unrelatedPost, "forest")
        ));
        postTagRepository.flush();

        mockMvc.perform(get("/api/posts/{id}/similar", testPostId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(similarPost.getId()))
                .andExpect(jsonPath("$[0].description").value("Waterfront sunset"))
                .andExpect(jsonPath("$[0].tags[0]").exists());
    }

    @Test
    public void getSimilarPosts_returnsEmptyListWhenNoSimilarPostsExist() throws Exception {
        postTagRepository.saveAll(List.of(
                new PostTag(null, post, "sunset"),
                new PostTag(null, post, "marsh")
        ));
        postTagRepository.flush();

        mockMvc.perform(get("/api/posts/{id}/similar", testPostId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

}

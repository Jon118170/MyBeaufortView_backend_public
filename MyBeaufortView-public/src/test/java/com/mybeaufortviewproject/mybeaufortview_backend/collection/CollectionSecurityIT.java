package com.mybeaufortviewproject.mybeaufortview_backend.collection;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import com.mybeaufortviewproject.mybeaufortview_backend.security.SecurityTestUtils;
import com.mybeaufortviewproject.mybeaufortview_backend.user.Role;
import com.mybeaufortviewproject.mybeaufortview_backend.user.User;
import com.mybeaufortviewproject.mybeaufortview_backend.user.UserRepository;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@Transactional
public class CollectionSecurityIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User owner;
    private User otherPriv;

    private String ownerToken;
    private String otherPrivToken;

    private Post ownersPost;

    @BeforeEach
    public void setup() {
        owner = SecurityTestUtils.seedUser(userRepository, passwordEncoder, "owner@example.com", Role.PRIVILEGED_USER);
        otherPriv = SecurityTestUtils.seedUser(userRepository, passwordEncoder, "other@example.com", Role.PRIVILEGED_USER);

        ownerToken = SecurityTestUtils.tokenFor(owner, jwtUtil);
        otherPrivToken = SecurityTestUtils.tokenFor(otherPriv, jwtUtil);

        Location location = new Location();
        location.setName("Hunting Island");
        location.setSlug("hunting-island-" + System.nanoTime());
        location.setLatitude(32.3738);
        location.setLongitude(-80.4512);
        location.setDescription("Test location for collection security tests");

        location = locationRepository.save(location);

        ownersPost = new Post();
        ownersPost.setUser(owner);
        ownersPost.setDescription("Owner's Post");
        ownersPost.setImageUrl("http://example.com/image.jpg");
        ownersPost.setCreatedAt(Instant.now());
        ownersPost.setLocation(location);

        ownersPost = postRepository.save(ownersPost);
    }

    // ----------------
    // POST /api/collections (authenticated)
    // ----------------

    @Test
    public void noToken_createCollection_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/collections")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"My Collection\",\"visibility\":\"PUBLIC\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void owner_createCollection_shouldReturn201() throws Exception {
        String payload = """
        { "title": "My Collection", "visibility": "PUBLIC" }
        """;

        mockMvc.perform(post("/api/collections")
                .with(SecurityTestUtils.bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("My Collection"))
            .andExpect(jsonPath("$.owner.id").value(owner.getId()));
    }

    // --------------
    // PUT /api/collections/{id} {authenticated + owner-only => 404 for non-owner)
    // --------------

    @Test
    public void owner_updateCollection_shouldReturn200() throws Exception {
        Collection c = new Collection();
        c.setCollectionName("Old TItle");
        c.setVisibility(CollectionVisibility.PUBLIC);
        c.setUser(owner);
        c = collectionRepository.save(c);

        String payload = """
                { "title": "New Title", "visibility": "PRIVATE" }
                 """;

        mockMvc.perform(put("/api/collections/{id}", c.getId())
                .with(SecurityTestUtils.bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("New Title"));
    }

    @Test
    public void nonOwner_updateCollection_shouldReturn404() throws Exception {
        Collection c = new Collection();
        c.setCollectionName("Owner Collection");
        c.setVisibility(CollectionVisibility.PUBLIC);
        c.setUser(owner);
        c = collectionRepository.save(c);

        String payload = """
                { "title": "Hacked Title", "visibility": "PUBLIC" }
                """;

        mockMvc.perform(put("/api/collections/{id}", c.getId())
                .with(SecurityTestUtils.bearer(otherPrivToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isNotFound());

    }

    @Test
    public void noToken_updateCollection_shouldReturn401() throws Exception {
        Collection c = new Collection();
        c.setCollectionName("Owner Collection");
        c.setVisibility(CollectionVisibility.PUBLIC);
        c.setUser(owner);
        c = collectionRepository.save(c);

        String payload = """
                { "title": "Hacked Title", "visibility": "PUBLIC" }
                """;

        mockMvc.perform(
                put("/api/collections/{id}", c.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void updateCollection_blankTitle_shouldReturn400() throws Exception {
        Collection c = new Collection();
        c.setCollectionName("Title Collection");
        c.setVisibility(CollectionVisibility.PUBLIC);
        c.setUser(owner);
        c = collectionRepository.save(c);

        String payload = """
                { "title": "", "visibility": "PUBLIC" }
                """;

        mockMvc.perform(put("/api/collections/{id}", c.getId())
                .with(SecurityTestUtils.bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest());
    }

    // -------------
    // POST /api/collections/{cid}/posts/{pid} (authenticated + owner-only => 404 for non-owner)
    // -------------

    @Test
    public void owner_addPostToCollection_shouldReturn204() throws Exception {
        Collection c = new Collection();
        c.setCollectionName("Owner Collection");
        c.setVisibility(CollectionVisibility.PUBLIC);
        c.setUser(owner);
        c = collectionRepository.save(c);

        mockMvc.perform(post("/api/collections/{cid}/posts/{pid}", c.getId(), ownersPost.getId())
                .with(SecurityTestUtils.bearer(ownerToken)))
            .andExpect(status().isNoContent());
    }

    @Test
    public void owner_addSamePostTwice_shouldStillReturn204() throws Exception {
        Collection c = new Collection();
        c.setCollectionName("Owner Collection");
        c.setVisibility(CollectionVisibility.PUBLIC);
        c.setUser(owner);
        c = collectionRepository.save(c);

        mockMvc.perform(post("/api/collections/{cid}/posts/{pid}", c.getId(), ownersPost.getId())
                .with(SecurityTestUtils.bearer(ownerToken)))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/collections/{cid}/posts/{pid}", c.getId(), ownersPost.getId())
                .with(SecurityTestUtils.bearer(ownerToken)))
            .andExpect(status().isNoContent());
    }

    @Test
    public void owner_addMissingPost_shouldReturn404() throws Exception {
        Collection c = new Collection();
        c.setCollectionName("Owner Collection");
        c.setVisibility(CollectionVisibility.PUBLIC);
        c.setUser(owner);
        c = collectionRepository.save(c);

        mockMvc.perform(
                post("/api/collections/{cid}/posts/{pid}", c.getId(), 999999L)
                .with(SecurityTestUtils.bearer(ownerToken)))
            .andExpect(status().isNotFound());
    }

    @Test
    public void nonOwner_addPostToCollection_shouldReturn404() throws Exception {
        Collection c = new Collection();
        c.setCollectionName("Owner Collection");
        c.setVisibility(CollectionVisibility.PUBLIC);
        c.setUser(owner);
        c = collectionRepository.save(c);

        mockMvc.perform(post("/api/collections/{cid}/posts/{pid}", c.getId(), ownersPost.getId())
                .with(SecurityTestUtils.bearer(otherPrivToken)))
            .andExpect(status().isNotFound());
    }

    @Test
    public void noToken_addPostToCollection_shouldReturn401() throws Exception {
        Collection c = new Collection();
        c.setCollectionName("My Collection");
        c.setVisibility(CollectionVisibility.PUBLIC);
        c.setUser(owner);
        c = collectionRepository.save(c);

        mockMvc.perform(post("/api/collections/{cid}/posts/{pid}", c.getId(), ownersPost.getId()))
                .andExpect(status().isUnauthorized());
    }


    // -------------
    // DELETE /api/collections/{cid}/posts/{pid} (authenticated + owner-only => 404 for non-owner; idempotent)
    // -------------

    @Test
    public void owner_removePostTwice_shouldStillReturn204() throws Exception {
        Collection c = new Collection();
        c.setCollectionName("Owner Collection");
        c.setVisibility(CollectionVisibility.PUBLIC);
        c.setUser(owner);
        c = collectionRepository.save(c);

        // add once
        mockMvc.perform(post("/api/collections/{cid}/posts/{pid}", c.getId(), ownersPost.getId())
                .with(SecurityTestUtils.bearer(ownerToken)))
            .andExpect(status().isNoContent());

        // remove
        mockMvc.perform(delete("/api/collections/{cid}/posts/{pid}", c.getId(), ownersPost.getId())
                .with(SecurityTestUtils.bearer(ownerToken)))
            .andExpect(status().isNoContent());

        // remove again (idempotent)
        mockMvc.perform(delete("/api/collections/{cid}/posts/{pid}", c.getId(), ownersPost.getId())
                .with(SecurityTestUtils.bearer(ownerToken)))
            .andExpect(status().isNoContent());
    }

    @Test
    public void nonOwner_removePost_shouldReturn404() throws Exception {
        Collection c = new Collection();
        c.setCollectionName("Owner Collection");
        c.setVisibility(CollectionVisibility.PUBLIC);
        c.setUser(owner);
        c = collectionRepository.save(c);

        mockMvc.perform(delete("/api/collections/{cid}/posts/{pid}", c.getId(), ownersPost.getId())
                .with(SecurityTestUtils.bearer(otherPrivToken)))
            .andExpect(status().isNotFound());
    }

    @Test
    public void noToken_removePost_shouldReturn401() throws Exception {
        Collection c = new Collection();
        c.setCollectionName("Owner Collection");
        c.setVisibility(CollectionVisibility.PUBLIC);
        c.setUser(owner);
        c = collectionRepository.save(c);

        mockMvc.perform(delete("/api/collections/{cid}/posts/{pid}", c.getId(), ownersPost.getId()))
                .andExpect(status().isUnauthorized());
    }

    // --------------
    // DELETE /api/collections/{id} (authenticated + owner-only => 404 for non-owner; idempotent and FK-safe)
    // --------------

    @Test
    public void owner_deleteCollection_shouldReturn204() throws Exception {
        Collection c = new Collection();
        c.setCollectionName("Owner Collection");
        c.setVisibility(CollectionVisibility.PUBLIC);
        c.setUser(owner);
        c = collectionRepository.save(c);

        // add entry
        mockMvc.perform(post("/api/collections/{cid}/posts/{pid}", c.getId(), ownersPost.getId())
                .with(SecurityTestUtils.bearer(ownerToken)))
            .andExpect(status().isNoContent());

        // delete collection (should not FK fail even though it has a post)
        mockMvc.perform(delete("/api/collections/{id}", c.getId())
                .with(SecurityTestUtils.bearer(ownerToken)))
            .andExpect(status().isNoContent());
    }

    @Test
    public void nonOwner_deleteCollection_shouldReturn404() throws Exception {
        Collection c = new Collection();
        c.setCollectionName("Owner Collection");
        c.setVisibility(CollectionVisibility.PUBLIC);
        c.setUser(owner);
        c = collectionRepository.save(c);

        mockMvc.perform(delete("/api/collections/{id}", c.getId())
                .with(SecurityTestUtils.bearer(otherPrivToken)))
            .andExpect(status().isNotFound());
    }

    @Test
    public void noToken_deleteCollection_shouldReturn401() throws Exception {
        Collection c = new Collection();
        c.setCollectionName("Owner Collection");
        c.setVisibility(CollectionVisibility.PUBLIC);
        c.setUser(owner);
        c = collectionRepository.save(c);

        mockMvc.perform(delete("/api/collections/{id}", c.getId()))
            .andExpect(status().isUnauthorized());
    }
}

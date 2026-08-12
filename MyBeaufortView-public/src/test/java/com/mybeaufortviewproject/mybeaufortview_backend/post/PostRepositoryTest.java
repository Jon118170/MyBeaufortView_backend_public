package com.mybeaufortviewproject.mybeaufortview_backend.post;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.mybeaufortviewproject.mybeaufortview_backend.location.Location;
import com.mybeaufortviewproject.mybeaufortview_backend.location.LocationRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.user.Role;
import com.mybeaufortviewproject.mybeaufortview_backend.user.User;
import com.mybeaufortviewproject.mybeaufortview_backend.user.UserRepository;

@ActiveProfiles("test")
@DataJpaTest
public class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private PostTagRepository postTagRepository;

    private User user;

    private Location location;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setUsername("photog1");
        user.setName("Jane Doe");
        user.setEmail("jane@example.com");
        user.setPassword("password");
        user.setRole(Role.PRIVILEGED_USER);
        user = userRepository.save(user);

        location = new Location();
        location.setName("Hunting Island");
        location.setSlug("hunting-island");
        location.setLatitude(32.3738);
        location.setLongitude(-80.4512);
        location.setDescription("Barrier island known for lighthouse views and marsh sunsets.");
        location = locationRepository.save(location);
    }

    @Test
    public void findSimilarPosts_returnsPostsOrderedBySharedTagCount() {
        // source post
        Post source = new Post();
        source.setUser(user);
        source.setDescription("Golden hour");
        source.setImageUrl("https://img/source.jpg");
        source.setLocation(location);
        source = postRepository.save(source);

        // similar post with 2 matching tags
        Post similar1 = new Post();
        similar1.setUser(user);
        similar1.setDescription("Marsh sunset");
        similar1.setImageUrl("https://img/similar1.jpg");
        similar1.setLocation(location);
        similar1 = postRepository.save(similar1);

        // similar post with 1 matching tag
        Post similar2 = new Post();
        similar2.setUser(user);
        similar2.setDescription("Sky only");
        similar2.setImageUrl("https://img/similar2.jpg");
        similar2.setLocation(location);
        similar2 = postRepository.save(similar2);

        // unrelated post
        Post unrelated = new Post();
        unrelated.setUser(user);
        unrelated.setDescription("Forest trail");
        unrelated.setImageUrl("https://img/unrelated.jpg");
        unrelated.setLocation(location);
        unrelated = postRepository.save(unrelated);

        postTagRepository.saveAll(List.of(
                new PostTag(null, source, "sunset"),
                new PostTag(null, source, "marsh"),

                new PostTag(null, similar1, "sunset"),
                new PostTag(null, similar1, "marsh"),

                new PostTag(null, similar2, "sunset"),

                new PostTag(null, unrelated, "forest")
        ));

        List<Post> results = postRepository.findSimilarPosts(source.getId());

        assertEquals(2, results.size());
        assertEquals(similar1.getId(), results.get(0).getId());
        assertEquals(similar2.getId(), results.get(1).getId());
    }

    @Test
    public void findSimilarPosts_excludesSourcePost() {
        Post source = savePost("Golden hour over the marsh", "https://img/source.jpg");

        saveTag(source, "sunset");
        saveTag(source, "marsh");

        List<Post> results = postRepository.findSimilarPosts(source.getId());

        assertTrue(results.stream().noneMatch(post -> post.getId().equals(source.getId())));
    }

    @Test
    public void findSimilarPosts_returnsEmptyWhenNoOtherPostsShareTags() {
        Post source = savePost("Golden hour over the marsh", "https://img/source.jpg");
        Post unrelated = savePost("Forest trail", "https://img/unrelated.jpg");

        saveTag(source, "sunset");
        saveTag(source, "marsh");

        saveTag(unrelated, "forest");

        List<Post> results = postRepository.findSimilarPosts(source.getId());

        assertTrue(results.isEmpty());
    }

    private Post savePost(String description, String imageUrl) {
        Post post = new Post();
        post.setUser(user);
        post.setDescription(description);
        post.setImageUrl(imageUrl);
        post.setLocation(location);
        return postRepository.save(post);
    }

    private void saveTag(Post post, String tag) {
        PostTag postTag = new PostTag();
        postTag.setPost(post);
        postTag.setTag(tag);
        postTagRepository.save(postTag);
    }
}

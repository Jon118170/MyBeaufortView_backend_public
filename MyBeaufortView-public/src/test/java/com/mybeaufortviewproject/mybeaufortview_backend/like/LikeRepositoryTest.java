package com.mybeaufortviewproject.mybeaufortview_backend.like;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.mybeaufortviewproject.mybeaufortview_backend.location.Location;
import com.mybeaufortviewproject.mybeaufortview_backend.location.LocationRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.post.Post;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.user.Role;
import com.mybeaufortviewproject.mybeaufortview_backend.user.User;
import com.mybeaufortviewproject.mybeaufortview_backend.user.UserRepository;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@DataJpaTest
public class LikeRepositoryTest {

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private LocationRepository locationRepository;

    private Post savePost(User user, String description) {
        Location location = new Location();
        location.setName("Hunting Island");
        location.setSlug("hunting-island-" + UUID.randomUUID());
        location.setLatitude(32.3738);
        location.setLongitude(-80.4512);
        location.setDescription("Test location for like repository tests");

        location = locationRepository.save(location);


        Post post = new Post();
        post.setUser(user);
        post.setDescription(description);
        post.setImageUrl("https://example.com/image.jpg");
        post.setLocation(location);

        return postRepository.save(post);
    }

    @Test
    public void testCreateAndFindLike() {

        // Create a new Like
        User user = new User("johndoe", "John Doe", "john@example.com", "password123", Role.PRIVILEGED_USER);
        user = userRepository.save(user);

        // Create a new Post
        Post post = savePost(user, "Test Post");

        // Create and save a like linking the user and post
        Like like = new Like();
        like.setUser(user);
        like.setPost(post);
        Like savedLike = likeRepository.save(like);

        // Retrieve the Like by ID
        Optional<Like> retrievedLike = likeRepository.findById(savedLike.getId());

        // Verify the Like was retrieved correctly
        assertTrue(retrievedLike.isPresent(), "Like should be present");
        assertEquals(user.getId(), retrievedLike.get().getUser().getId());
        assertEquals(post.getId(), retrievedLike.get().getPost().getId());
    }

    // Test for custom query: existsByUser_IdAndPost_Id
    @Test
    public void testExistsByUser_IdAndPost_Id() {
        // Create and save a User
        User user = new User("janedoe", "Jane Doe", "jane@example.com", "password456", Role.PRIVILEGED_USER);
        user = userRepository.save(user);

        // Create and save a Post
        Post post = savePost(user, "Another Test Post");

        // Create and save a Like linking the User and Post
        Like like = new Like();
        like.setUser(user);
        like.setPost(post);
        likeRepository.save(like);

        // Check existence of the Like
        boolean exists = likeRepository.existsByUser_IdAndPost_Id(user.getId(), post.getId());
        assertTrue(exists, "Like should exist for the given user and post");

        // Check for a non-existent combination
        boolean notExists = likeRepository.existsByUser_IdAndPost_Id(999L, 999L);
        assertFalse(notExists, "Like should not exist for invalid user/post IDs");
    }

    // Delete likes from posts
    @Test
    public void testDeleteByUserIdAndPostId() {
        User user = userRepository.save(
                new User("deleteuser2", "Delete User2", "delete2@example.com", "password44", Role.PRIVILEGED_USER));

        Post post = savePost(user, "Post to be unliked");

        Like like = new Like();
        like.setUser(user);
        like.setPost(post);
        likeRepository.save(like);

        // Act: delete by (userId, postId)
        likeRepository.deleteByUser_IdAndPost_Id(user.getId(), post.getId());

        // Assert: verify deletion
        assertFalse(likeRepository.existsByUser_IdAndPost_Id(user.getId(), post.getId()), "Like should be deleted");
    }

    @Test
    public void testCountByPostId() {
        User user = userRepository
                .save(new User("countuser", "Count User", "count@example.com", "password55", Role.PRIVILEGED_USER));

        Post post = savePost(user, "Post to be liked");

        Like like = new Like();
        like.setUser(user);
        like.setPost(post);
        likeRepository.save(like);

        long count = likeRepository.countByPost_Id(post.getId());
        assertEquals(1, count, "Like count should be 1");
    }
}

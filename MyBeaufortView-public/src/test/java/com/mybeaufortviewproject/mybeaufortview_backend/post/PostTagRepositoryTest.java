package com.mybeaufortviewproject.mybeaufortview_backend.post;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
class PostTagRepositoryTest {

    @Autowired
    private PostTagRepository postTagRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocationRepository locationRepository;

    private Post post1;
    private Post post2;

    @BeforeEach
    public void setUp() {
        User user = new User();
        user.setUsername("photog1");
        user.setName("Jane Doe");
        user.setEmail("jane@example.com");
        user.setPassword("password");
        user.setRole(Role.PRIVILEGED_USER);
        user = userRepository.save(user);

        Location location = new Location();
        location.setName("Hunting Island");
        location.setSlug("hunting-island");
        location.setLatitude(32.3738);
        location.setLongitude(-80.4512);
        location.setDescription("Barrier island");
        location = locationRepository.save(location);

        post1 = new Post();
        post1.setUser(user);
        post1.setDescription("Sunset");
        post1.setImageUrl("https://img/1.jpg");
        post1.setLocation(location);
        post1 = postRepository.save(post1);

        post2 = new Post();
        post2.setUser(user);
        post2.setDescription("Wildlife");
        post2.setImageUrl("https://img/2.jpg");
        post2.setLocation(location);
        post2 = postRepository.save(post2);
    }

    @Test
    public void findByPost_Id_returnsTagsForSinglePost() {
        PostTag tag1 = new PostTag();
        tag1.setPost(post1);
        tag1.setTag("sunset");

        PostTag tag2 = new PostTag();
        tag2.setPost(post1);
        tag2.setTag("marsh");

        PostTag tag3 = new PostTag();
        tag3.setPost(post2);
        tag3.setTag("wildlife");

        postTagRepository.saveAll(List.of(tag1, tag2, tag3));

        List<PostTag> result = postTagRepository.findByPost_Id(post1.getId());

        assertEquals(2, result.size());
    }

    @Test
    public void findByPost_IdIn_returnsTagsForMultiplePosts() {
        PostTag tag1 = new PostTag();
        tag1.setPost(post1);
        tag1.setTag("sunset");

        PostTag tag2 = new PostTag();
        tag2.setPost(post2);
        tag2.setTag("wildlife");

        postTagRepository.saveAll(List.of(tag1, tag2));

        List<PostTag> result = postTagRepository.findByPost_IdIn(List.of(post1.getId(), post2.getId()));

        assertEquals(2, result.size());
    }

    @Test
    public void deleteByPost_Id_removesTagsForThatPostOnly() {
        PostTag tag1 = new PostTag();
        tag1.setPost(post1);
        tag1.setTag("sunset");

        PostTag tag2 = new PostTag();
        tag2.setPost(post2);
        tag2.setTag("wildlife");

        postTagRepository.saveAll(List.of(tag1, tag2));

        postTagRepository.deleteAllByPostId(post1.getId());

        List<PostTag> remainingPost1Tags = postTagRepository.findByPost_Id(post1.getId());
        List<PostTag> remainingPost2Tags = postTagRepository.findByPost_Id(post2.getId());

        assertEquals(0, remainingPost1Tags.size());
        assertEquals(1, remainingPost2Tags.size());
    }
}

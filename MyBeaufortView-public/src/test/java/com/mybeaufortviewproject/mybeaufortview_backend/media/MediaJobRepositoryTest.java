package com.mybeaufortviewproject.mybeaufortview_backend.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

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
class MediaJobRepositoryTest {

    @Autowired
    private MediaJobRepository mediaJobRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Test
    public void findByPostId_shouldReturnJobsForPost() {
        User user = createUser("tester1", "tester1@example.com");

        Location location = createLocation(
                "Hunting Island",
                "hunting-island",
                32.384,
                -80.669
        );

        Post post = createPost(user, location, "Golden hour over the marsh", "https://example.com/image1.jpg");

        MediaJob job = new MediaJob();
        job.setPost(post);
        job.setJobType(MediaJobType.AI_TAGGING);
        job.setStatus(MediaJobStatus.PENDING);
        mediaJobRepository.save(job);

        List<MediaJob> jobs = mediaJobRepository.findByPostId(post.getId());

        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).getPost().getId()).isEqualTo(post.getId());
        assertThat(jobs.get(0).getJobType()).isEqualTo(MediaJobType.AI_TAGGING);
    }

    @Test
    public void findTopByPostIdAndJobTypeOrderByCreatedAtDesc_shouldReturnLatestJob() {
        User user = createUser("tester2", "tester2@example.com");

        Location location = createLocation(
                "Waterfront Park",
                "waterfront-park",
                32.379,
                -80.692
        );

        Post post = createPost(user, location, "Waterfront view", "https://example.com/image2.jpg");

        MediaJob older = new MediaJob();
        older.setPost(post);
        older.setJobType(MediaJobType.AI_TAGGING);
        older.setStatus(MediaJobStatus.FAILED);
        mediaJobRepository.save(older);

        MediaJob newer = new MediaJob();
        newer.setPost(post);
        newer.setJobType(MediaJobType.AI_TAGGING);
        newer.setStatus(MediaJobStatus.COMPLETED);
        mediaJobRepository.save(newer);

        Optional<MediaJob> result =
                mediaJobRepository.findTopByPostIdAndJobTypeOrderByIdDesc(
                        post.getId(),
                        MediaJobType.AI_TAGGING
                );

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(MediaJobStatus.COMPLETED);
    }

    @Test
    public void findPendingWithPost_shouldReturnMatchingJobs() {
        User user = createUser("tester3", "tester3@example.com");

        Location location = createLocation(
                "Spanish Moss Trail",
                "spanish-moss-trail",
                32.400,
                -80.700
        );

        Post post = createPost(user, location, "Trail photo", "https://example.com/image3.jpg");

        MediaJob pendingJob = new MediaJob();
        pendingJob.setPost(post);
        pendingJob.setJobType(MediaJobType.THUMBNAIL_GENERATION);
        pendingJob.setStatus(MediaJobStatus.PENDING);
        mediaJobRepository.save(pendingJob);

        List<MediaJob> jobs = mediaJobRepository.findPendingWithPost(MediaJobStatus.PENDING);

        assertThat(jobs).isNotEmpty();
        assertThat(jobs).allMatch(job -> job.getStatus() == MediaJobStatus.PENDING);
    }

    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password");
        user.setName("Test User" + username);
        user.setRole(Role.PRIVILEGED_USER);
        return userRepository.save(user);
    }

    private Location createLocation(String name, String slug, Double latitude, Double longitude) {
        Location location = new Location();
        location.setName(name);
        location.setSlug(slug);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setDescription("Test location");
        return locationRepository.save(location);
    }

    private Post createPost(User user, Location location, String description, String imageUrl) {
        Post post = new Post();
        post.setUser(user);
        post.setLocation(location);
        post.setDescription(description);
        post.setImageUrl(imageUrl);
        return postRepository.save(post);
    }
}

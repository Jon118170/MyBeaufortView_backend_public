package com.mybeaufortviewproject.mybeaufortview_backend.post;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.PostNotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.UserPrincipal;
import com.mybeaufortviewproject.mybeaufortview_backend.like.LikeRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.location.Location;
import com.mybeaufortviewproject.mybeaufortview_backend.location.LocationRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.media.MediaJobService;
import com.mybeaufortviewproject.mybeaufortview_backend.media.MediaJobType;
import com.mybeaufortviewproject.mybeaufortview_backend.media.PhotoTaggingService;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.PostCreateRequest;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.PostResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.PostUpdateRequest;
import com.mybeaufortviewproject.mybeaufortview_backend.user.Role;
import com.mybeaufortviewproject.mybeaufortview_backend.user.User;
import com.mybeaufortviewproject.mybeaufortview_backend.user.UserRepository;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private PhotoTaggingService photoTaggingService;

    @Mock
    private MediaJobService mediaJobService;


    @InjectMocks
    private PostService postService;

    private Post sourcePost;
    private Post similarPost;
    private User user;
    private Location location;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setId(10L);
        user.setUsername("photog1");
        user.setName("Jane Doe");
        user.setEmail("jane@example.com");
        user.setRole(Role.PRIVILEGED_USER);

        location = new Location();
        location.setId(1L);
        location.setName("Hunting Island");
        location.setSlug("hunting-island");
        location.setLatitude(32.3738);
        location.setLongitude(-80.4512);
        location.setDescription("Barrier island known for lighthouse views and marsh sunsets.");

        sourcePost = new Post();
        sourcePost.setId(1L);
        sourcePost.setUser(user);
        sourcePost.setDescription("Golden hour over the marsh");
        sourcePost.setImageUrl("https://example.com/source.jpg");
        sourcePost.setCreatedAt(Instant.parse("2024-01-01T18:30:00Z"));
        sourcePost.setLocation(location);

        similarPost = new Post();
        similarPost.setId(2L);
        similarPost.setUser(user);
        similarPost.setDescription("Waterfront sunset");
        similarPost.setImageUrl("https://example.com/similar.jpg");
        similarPost.setCreatedAt(Instant.parse("2024-01-02T18:30:00Z"));
        similarPost.setLocation(location);
    }

    @Test
    public void getSimilarPosts_returnsEmptyListWhenSourcePostHasNoTags() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(sourcePost));
        when(photoTaggingService.getTagsForPost(1L)).thenReturn(List.of());

        List<PostResponse> result = postService.getSimilarPosts(1L, null);

        assertTrue(result.isEmpty());
        verify(postRepository, never()).findSimilarPosts(anyLong());
    }

    @Test
    public void getSimilarPosts_returnsMappedDtosWhenSimilarPostsExist() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(sourcePost));
        when(photoTaggingService.getTagsForPost(1L)).thenReturn(List.of("sunset", "marsh"));
        when(postRepository.findSimilarPosts(1L)).thenReturn(List.of(similarPost));

        when(photoTaggingService.getTagsForPostIds(List.of(2L)))
                .thenReturn(Map.of(2L, List.of("sunset", "water")));

        when(likeRepository.countByPost_Id(2L)).thenReturn(0L);

        List<PostResponse> result = postService.getSimilarPosts(1L, null);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).id());
        assertEquals("Waterfront sunset", result.get(0).description());
        assertEquals("hunting-island", result.get(0).location().slug());
        assertEquals(List.of("sunset", "water"), result.get(0).tags());
        assertFalse(result.get(0).likedByMe());
    }

    @Test
    void getSimilarPosts_throwsWhenPostDoesNotExist() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class,
                () -> postService.getSimilarPosts(999L, null));
    }

    @Test
    public void createPost_shouldCreatePostAndEnqueueAiTaggingJob() {
        PostCreateRequest request = new PostCreateRequest();
        request.setDescription("Golden hour over the marsh");
        request.setImageUrl("https://example.com/source.jpg");
        request.setLocationId(1L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(postRepository.existsByUser_IdAndDescription(10L, "Golden hour over the marsh"))
                .thenReturn(false);

        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            post.setId(100L);
            return post;
        });

        when(likeRepository.countByPost_Id(100L)).thenReturn(0L);
        when(photoTaggingService.getTagsForPost(100L)).thenReturn(List.of());

        PostResponse result = postService.createPost(request, 10L);

        assertNotNull(result);
        assertEquals(100L, result.id());
        assertEquals("Golden hour over the marsh", result.description());
        assertEquals("https://example.com/source.jpg", result.imageUrl());

        verify(mediaJobService).createJob(any(Post.class), eq(MediaJobType.AI_TAGGING));
    }

    @Test
    public void updatePost_shouldUpdatePostAndEnqueueAiTaggingJob() {
        PostUpdateRequest request = new PostUpdateRequest();
        request.setDescription("Updated marsh sunset");
        request.setImageUrl("https://example.com/updated.jpg");
        request.setLocationId(1L);

        UserPrincipal principal = new UserPrincipal(
                10L,
                "jane@example.com",
                "PRIVILEGED_USER"
        );

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(postRepository.findById(1L)).thenReturn(Optional.of(sourcePost));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(likeRepository.countByPost_Id(1L)).thenReturn(0L);
        when(likeRepository.existsByUser_IdAndPost_Id(10L, 1L)).thenReturn(false);
        when(photoTaggingService.getTagsForPost(1L)).thenReturn(List.of());

        PostResponse result = postService.updatePost(1L, request, principal);

        assertNotNull(result);
        assertEquals("Updated marsh sunset", result.description());
        assertEquals("https://example.com/updated.jpg", result.imageUrl());

        verify(mediaJobService).createJob(any(Post.class), eq(MediaJobType.AI_TAGGING));
    }
}

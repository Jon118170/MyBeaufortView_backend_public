package com.mybeaufortviewproject.mybeaufortview_backend.like;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.PostNotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.UserNotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.UserPrincipal;
import com.mybeaufortviewproject.mybeaufortview_backend.notification.NotificationService;
import com.mybeaufortviewproject.mybeaufortview_backend.notification.NotificationType;
import com.mybeaufortviewproject.mybeaufortview_backend.post.Post;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.user.Role;
import com.mybeaufortviewproject.mybeaufortview_backend.user.User;
import com.mybeaufortviewproject.mybeaufortview_backend.user.UserRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.user.UserService;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private LikeService likeService;

    @Captor
    private ArgumentCaptor<Like> likeCaptor;

    private User postOwner;
    private User likingUser;
    private Post post;

    private UserPrincipal principal;
    private UserPrincipal ownerPrincipal;

    @BeforeEach
    public void setUp() {
        postOwner = new User();
        postOwner.setId(1L);
        postOwner.setUsername("owner1");
        postOwner.setName("Post Owner");
        postOwner.setEmail("owner@example.com");
        postOwner.setPassword("password");
        postOwner.setRole(Role.PRIVILEGED_USER);

        likingUser = new User();
        likingUser.setId(2L);
        likingUser.setUsername("photofan");
        likingUser.setName("Photo Fan");
        likingUser.setEmail("fan@example.com");
        likingUser.setPassword("password");
        likingUser.setRole(Role.PRIVILEGED_USER);

        post = new Post();
        post.setId(10L);
        post.setUser(postOwner);

        principal = mock(UserPrincipal.class);
        ownerPrincipal = mock(UserPrincipal.class);
    }

    @Test
    public void likePost_shouldCreateLikeAndNotificationForPostOwner() {
        when(principal.id()).thenReturn(2L);

        when(likeRepository.existsByUser_IdAndPost_Id(2L, 10L)).thenReturn(false);
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userRepository.findById(2L)).thenReturn(Optional.of(likingUser));

        likeService.likePost(10L, principal);

        verify(likeRepository).save(likeCaptor.capture());
        Like savedLike = likeCaptor.getValue();

        assertEquals(post, savedLike.getPost());
        assertEquals(likingUser, savedLike.getUser());

        verify(notificationService).createNotification(
                postOwner,
                NotificationType.PHOTO_LIKED,
                "New like on your post",
                "photofan liked your post.",
                10L,
                null
        );
    }

    @Test
    public void likePost_shouldNotCreateNotificationWhenUserLikesOwnPost() {
        when(ownerPrincipal.id()).thenReturn(1L);

        when(likeRepository.existsByUser_IdAndPost_Id(1L, 10L)).thenReturn(false);
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(postOwner));

        likeService.likePost(10L, ownerPrincipal);

        verify(likeRepository).save(likeCaptor.capture());
        verify(notificationService, never()).createNotification(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    public void likePost_shouldDoNothingWhenLikeAlreadyExists() {
        when(principal.id()).thenReturn(2L);

        when(likeRepository.existsByUser_IdAndPost_Id(2L, 10L)).thenReturn(true);

        likeService.likePost(10L, principal);

        verify(likeRepository, never()).save(any());
        verify(postRepository, never()).findById(anyLong());
        verify(userRepository, never()).findById(anyLong());
        verify(notificationService, never()).createNotification(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    public void likePost_shouldThrowWhenPostNotFound() {
        when(principal.id()).thenReturn(2L);

        when(likeRepository.existsByUser_IdAndPost_Id(2L, 10L)).thenReturn(false);
        when(postRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> likeService.likePost(10L, principal));
    }

    @Test
    public void likePost_shouldThrowWhenUserNotFound() {
        when(principal.id()).thenReturn(2L);

        when(likeRepository.existsByUser_IdAndPost_Id(2L, 10L)).thenReturn(false);
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> likeService.likePost(10L, principal));
    }

    @Test
    public void unlikePost_shouldDeleteLike() {
        when(principal.id()).thenReturn(2L);

        likeService.unlikePost(10L, principal);

        verify(likeRepository).deleteByUser_IdAndPost_Id(2L, 10L);
    }

    @Test
    public void hasLiked_shouldReturnRepositoryResult() {
        when(principal.id()).thenReturn(2L);

        when(likeRepository.existsByUser_IdAndPost_Id(2L, 10L)).thenReturn(true);

        boolean result = likeService.hasLiked(10L, principal);

        assertTrue(result);
    }

    @Test
    public void countLikes_shouldReturnRepositoryCount() {
        when(likeRepository.countByPost_Id(10L)).thenReturn(5L);

        long result = likeService.countLikes(10L);

        assertEquals(5L, result);
    }
}

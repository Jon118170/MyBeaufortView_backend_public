package com.mybeaufortviewproject.mybeaufortview_backend.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import com.mybeaufortviewproject.mybeaufortview_backend.collection.CollectionRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.UserNotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostService;
import com.mybeaufortviewproject.mybeaufortview_backend.user.dto.UserProfileResponse;

@ActiveProfiles("test")
public class UserProfileServiceTest {

    @Test
    public void getProfile_throwsWhenUserMissing() {
        UserRepository userRepository = mock(UserRepository.class);
        PostRepository postRepository = mock(PostRepository.class);
        CollectionRepository collectionRepository = mock(CollectionRepository.class);
        PostService postService = mock(PostService.class);

        UserProfileService service =
                new UserProfileService(userRepository, postRepository, collectionRepository, postService);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.getProfile(99L));
    }

    @Test
    public void getProfile_returnsCountsAndSharePath() {
        UserRepository userRepository = mock(UserRepository.class);
        PostRepository postRepository = mock(PostRepository.class);
        CollectionRepository collectionRepository = mock(CollectionRepository.class);
        PostService postService = mock(PostService.class);

        UserProfileService service =
                new UserProfileService(userRepository, postRepository, collectionRepository, postService);

        User user = new User("photog1", "Jane Doe", "jane@example.com", "pw", Role.PRIVILEGED_USER);
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(postRepository.countByUser_Id(1L)).thenReturn(12L);
        when(collectionRepository.countByUser_Id(1L)).thenReturn(3L);

        UserProfileResponse response = service.getProfile(1L);

        assertEquals(1L, response.id());
        assertEquals("photog1", response.username());
        assertEquals("Jane Doe", response.name());
        assertEquals(12L, response.postCount());
        assertEquals(3L, response.collectionCount());
        assertEquals("/users/1", response.sharePath());
    }
}

package com.mybeaufortviewproject.mybeaufortview_backend.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.mybeaufortviewproject.mybeaufortview_backend.collection.CollectionRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.UserNotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.UserPrincipal;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostService;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.PostResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.user.dto.UserProfileResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.user.dto.UserProfileUpdateRequest;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CollectionRepository collectionRepository;
    private final PostService postService;

    public UserProfileService(
            UserRepository userRepository,
            PostRepository postRepository,
            CollectionRepository collectionRepository,
            PostService postService
    ) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.collectionRepository = collectionRepository;
        this.postService = postService;
    }

    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return mapToUserProfileResponse(user);
    }

     public UserProfileResponse  updateMyProfile(
             UserPrincipal principal,
             UserProfileUpdateRequest request
    ) {
         User user = userRepository.findById(principal.id())
                 .orElseThrow(() -> new UserNotFoundException(principal.id()));

        user.setName(request.name());
        user.setBio(request.bio() == null ? null : request.bio().trim());
        user.setProfileImageUrl(request.profileImageUrl() == null ? null : request.profileImageUrl().trim());

        User saved = userRepository.save(user);

        return mapToUserProfileResponse(saved);
     }

     public Page<PostResponse> getUserPosts(Long userId, Pageable pageable, UserPrincipal principal) {
            // Important: don’t silently return empty page for nonexistent user
            if (!userRepository.existsById(userId)) {
                throw new UserNotFoundException(userId);
            }

            // Fetch paginated posts for the user
            var posts = postRepository.findByUser_Id(userId, pageable);

            // Reuse your sealed PostService mapping logic (likedByMe, likeCount, author summary)
            return postService.toPostResponsePage(posts, principal);
        }

        private UserProfileResponse mapToUserProfileResponse(User user) {
            long postCount = postRepository.countByUser_Id(user.getId());
            long collectionCount = collectionRepository.countByUser_Id(user.getId());

            return new UserProfileResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getName(),
                    user.getBio(),
                    user.getProfileImageUrl(),
                    user.getCreatedAt(),
                    postCount,
                    collectionCount,
                    "/users/" + user.getId()
            );
        }
}

package com.mybeaufortviewproject.mybeaufortview_backend.like;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.ErrorCode;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.PostNotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.UnauthorizedException;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.UserNotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.UserPrincipal;
import com.mybeaufortviewproject.mybeaufortview_backend.notification.NotificationService;
import com.mybeaufortviewproject.mybeaufortview_backend.notification.NotificationType;
import com.mybeaufortviewproject.mybeaufortview_backend.post.Post;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.user.User;
import com.mybeaufortviewproject.mybeaufortview_backend.user.UserRepository;

@Service
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public LikeService(LikeRepository likeRepository, PostRepository postRepository, UserRepository userRepository, NotificationService notificationService) {
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void likePost(Long postId, UserPrincipal principal) {

        Long userId = requireUserId(principal);

        if (likeRepository.existsByUser_IdAndPost_Id(userId, postId)) {
            return;
        }

        // Fetch the post and user entities
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        // Fetch the user entity
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Create and save the like
        Like like = new Like();
        like.setPost(post);
        like.setUser(user);

        // Save the like to the database
        likeRepository.save(like);

        if (!post.getUser().getId().equals(userId)) {
            notificationService.createNotification(
                    post.getUser(),
                    NotificationType.PHOTO_LIKED,
                    "New like on your post",
                    user.getUsername() + " liked your post.",
                    post.getId(),
                    null
            );
        }

    }

    @Transactional
    public void unlikePost(Long postId, UserPrincipal principal) {
        // Remove the like if it exists
        Long userId = requireUserId(principal);
        likeRepository.deleteByUser_IdAndPost_Id(userId, postId);
    }

    @Transactional(readOnly = true)
    public boolean hasLiked(Long postId, UserPrincipal principal) {
        // Check if a like exists for the given user and post
        Long userId = requireUserId(principal);
        return likeRepository.existsByUser_IdAndPost_Id(userId, postId);
    }

    @Transactional(readOnly = true)
    public long countLikes(Long postId) {
        // Count the number of likes for the given post
        return likeRepository.countByPost_Id(postId);
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null || principal.id() == null) {
            throw new UnauthorizedException(
                    ErrorCode.AUTH_REQUIRED,
                    "Authenticated user not found");
        }

        return principal.id();
    }
}

package com.mybeaufortviewproject.mybeaufortview_backend.post;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.ConflictException;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.ErrorCode;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.ForbiddenException;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.LocationNotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.PostNotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.UserNotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.UserPrincipal;
import com.mybeaufortviewproject.mybeaufortview_backend.like.LikeRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.location.Location;
import com.mybeaufortviewproject.mybeaufortview_backend.location.LocationRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.location.dto.LocationResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.media.MediaJobService;
import com.mybeaufortviewproject.mybeaufortview_backend.media.MediaJobType;
import com.mybeaufortviewproject.mybeaufortview_backend.media.PhotoTaggingService;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.AuthorResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.PostCreateRequest;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.PostResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.PostUpdateRequest;
import com.mybeaufortviewproject.mybeaufortview_backend.user.Role;
import com.mybeaufortviewproject.mybeaufortview_backend.user.User;
import com.mybeaufortviewproject.mybeaufortview_backend.user.UserRepository;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final PhotoTaggingService photoTaggingService;
    private final MediaJobService mediaJobService;


    public PostService(
            PostRepository postRepository,
            LikeRepository likeRepository,
            UserRepository userRepository,
            LocationRepository locationRepository,
            PhotoTaggingService photoTaggingService,
            MediaJobService mediaJobService
    ) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.photoTaggingService = photoTaggingService;
        this.mediaJobService = mediaJobService;
    }

    public PostResponse createPost(PostCreateRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new LocationNotFoundException(request.getLocationId()));

        if (postRepository.existsByUser_IdAndDescription(userId, request.getDescription())) {
            throw new ConflictException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "Duplicate description for this user!"
            );
        }

        Post newPost = new Post();
        newPost.setUser(user);
        newPost.setDescription(request.getDescription());
        newPost.setImageUrl(request.getImageUrl());
        newPost.setLocation(location);

        Post savedPost = postRepository.save(newPost);
        mediaJobService.createJob(savedPost, MediaJobType.THUMBNAIL_GENERATION);
        mediaJobService.createJob(savedPost, MediaJobType.AI_TAGGING);

        return toPostResponse(savedPost, null);
    }

    public Post savePost(Post post, UserPrincipal principal) {
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new UserNotFoundException(principal.id()));

        post.setUser(user);
        return postRepository.save(post);
    }

    public Post getPostById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException(id));
    }

    public PostResponse getPostResponseById(Long id, UserPrincipal principalOrNull) {
        Post post = getPostById(id);
        return toPostResponse(post, principalOrNull);
    }

    public List<PostResponse> getAllPosts(UserPrincipal principal) {
        return toPostResponseList(postRepository.findAll(), principal);
    }

    // Delete post with authorization check
    @Transactional
    public void deletePost(Long id, UserPrincipal principal) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException(id));

        Long userId = principal.id();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isOwner = post.getUser().getId().equals(userId);

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException(
                    "You do not have permission to delete this post."
            );
        }

        postRepository.delete(post);
    }

    // Update post with authorization check, and return updated PostResponse with like count, likedByMe, and tags
    public PostResponse updatePost(Long id, PostUpdateRequest request, UserPrincipal principal) {
        Post existingPost = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException(id));

        Long userId = principal.id();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isOwner = existingPost.getUser().getId().equals(userId);

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException(
                    "You do not have permission to update this post."
            );
        }

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new LocationNotFoundException(request.getLocationId()));

        existingPost.setDescription(request.getDescription());
        existingPost.setImageUrl(request.getImageUrl());
        existingPost.setLocation(location);

        Post updatedPost = postRepository.save(existingPost);

        mediaJobService.createJob(updatedPost, MediaJobType.THUMBNAIL_GENERATION);
        mediaJobService.createJob(updatedPost, MediaJobType.AI_TAGGING);

        return toPostResponse(updatedPost, principal);
    }

    // Search posts by keyword in description or tags, with sorting and including like count, likedByMe, and tags
    public List<Post> searchPosts(String keyword, String sortDirection) {
         if (keyword == null || keyword.trim().isEmpty()) {
             return List.of();
         }

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return postRepository.searchByDescriptionOrTag(
                keyword.trim(),
                Sort.by(direction, "createdAt")
        );
    }

    // Search posts by keyword and return PostResponse with like count, likedByMe, and tags
    public List<PostResponse> searchPostResponses(String keyword, String sortDirection) {
        return toPostResponseList(searchPosts(keyword, sortDirection), null);

    }

    // Get feed with pagination, including like count, likedByMe, and tags
    public Page<PostResponse> getFeed(Pageable pageable, UserPrincipal principal) {
        Page<Post> posts = postRepository.findAll(pageable);
        return toPostResponsePage(posts, principal);
    }

    // Get posts by location slug with pagination, including like count, likedByMe, and tags
    public Page<PostResponse> getPostsByLocationSlug(String slug, Pageable pageable, UserPrincipal principal) {
        locationRepository.findBySlug(slug)
                .orElseThrow(() -> new LocationNotFoundException(slug));

        Page<Post> posts = postRepository.findByLocation_Slug(slug, pageable);
        return toPostResponsePage(posts, principal);
    }

    // Helper method to convert a Page of Post entities to a Page of PostResponse, including like count, likedByMe, and tags
    public Page<PostResponse> toPostResponsePage(Page<Post> posts, UserPrincipal principal) {
        List<Post> content = posts.getContent();

        if (content.isEmpty()) {
            return posts.map(post -> toPostResponse(post, principal));
        }

        List<Long> postIds = content.stream()
                .map(Post::getId)
                .toList();

        Map<Long, List<String>> tagsByPostId = photoTaggingService.getTagsForPostIds(postIds);

        List<PostResponse> responseContent = content.stream()
                .map(post -> toPostResponse(post, principal, tagsByPostId))
                .toList();

        return new PageImpl<>(
                responseContent,
                posts.getPageable(),
                posts.getTotalElements()
        );
    }

    // Get similar posts based on shared tags, including like count, likedByMe, and tags
    public List<PostResponse> getSimilarPosts(Long postId, UserPrincipal principal) {

        getPostById(postId); // validate existence

        List<String> sourceTags = photoTaggingService.getTagsForPost(postId);
        if (sourceTags.isEmpty()) {
            return List.of();
        }

        List<Post> similarPosts = postRepository.findSimilarPosts(postId);

        return toPostResponseList(
                similarPosts.stream().limit(10).toList(),
                principal
            );
    }

    // Helper method to convert a list of Post entities to a list of PostResponse, including like count, likedByMe, and tags
    private List<PostResponse> toPostResponseList(List<Post> posts, UserPrincipal principal) {
        if (posts == null || posts.isEmpty()) {
            return List.of();
        }

        List<Long> postIds = posts.stream()
                .map(Post::getId)
                .toList();

        Map<Long, List<String>> tagsByPostId = photoTaggingService.getTagsForPostIds(postIds);

        return posts.stream()
                .map(post -> toPostResponse(post, principal, tagsByPostId))
                .toList();
    }

    // Overloaded helper method to convert Post entity to PostResponse, using pre-fetched tags for efficiency
    private PostResponse toPostResponse(
            Post post,
            UserPrincipal principalOrNull,
            Map<Long, List<String>> tagsByPostId
    ) {
        long likeCount = likeRepository.countByPost_Id(post.getId());

        boolean likedByMe = principalOrNull != null
                && likeRepository.existsByUser_IdAndPost_Id(principalOrNull.id(), post.getId());

        List<String> tags = tagsByPostId.getOrDefault(post.getId(), List.of());

        return new PostResponse(
                post.getId(),
                post.getDescription(),
                post.getImageUrl(),
                post.getThumbnailUrl(),
                post.getCreatedAt(),
                toAuthorResponse(post.getUser()),
                toLocationResponse(post.getLocation()),
                likeCount,
                likedByMe,
                tags
        );
    }

    // Helper method to convert Post entity to PostResponse, including like count and likedByMe
    private PostResponse toPostResponse(Post post, UserPrincipal principalOrNull) {
        long likeCount = likeRepository.countByPost_Id(post.getId());

        boolean likedByMe = principalOrNull != null
                && likeRepository.existsByUser_IdAndPost_Id(principalOrNull.id(), post.getId());

        List<String> tags = photoTaggingService.getTagsForPost(post.getId());

        return new PostResponse(
                post.getId(),
                post.getDescription(),
                post.getImageUrl(),
                post.getThumbnailUrl(),
                post.getCreatedAt(),
                toAuthorResponse(post.getUser()),
                toLocationResponse(post.getLocation()),
                likeCount,
                likedByMe,
                tags
        );
    }

    // Helper methods to convert entities to responses
    private AuthorResponse toAuthorResponse(User user) {
        if (user == null) {
            return null;
        }

        return new AuthorResponse(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getProfileImageUrl()
        );
    }

    // Helper method to convert Location entity to LocationResponse
    private LocationResponse toLocationResponse(Location location) {
        if (location == null) {
            return null;
        }

        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getSlug(),
                location.getLatitude(),
                location.getLongitude(),
                location.getDescription(),
                0L
        );
    }

}

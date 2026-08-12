package com.mybeaufortviewproject.mybeaufortview_backend.collection;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.mybeaufortviewproject.mybeaufortview_backend.collection.dto.CollectionCreateRequest;
import com.mybeaufortviewproject.mybeaufortview_backend.collection.dto.CollectionDetailResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.collection.dto.CollectionResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.collection.dto.CollectionUpdateRequest;
import com.mybeaufortviewproject.mybeaufortview_backend.common.dto.PageResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.UserNotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.UserPrincipal;
import com.mybeaufortviewproject.mybeaufortview_backend.like.LikeRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.location.dto.LocationResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.media.PhotoTaggingService;
import com.mybeaufortviewproject.mybeaufortview_backend.post.Post;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.AuthorResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.PostResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.user.User;
import com.mybeaufortviewproject.mybeaufortview_backend.user.UserRepository;

@Service
public class CollectionService {

    private final CollectionEntryRepository collectionEntryRepository;
    private final LikeRepository likeRepository;
    private final CollectionRepository collectionRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PhotoTaggingService photoTaggingService;

    public CollectionService(CollectionEntryRepository collectionEntryRepository, LikeRepository likeRepository,
            CollectionRepository collectionRepository, UserRepository userRepository, PostRepository postRepository,
            PhotoTaggingService photoTaggingService) {

        this.userRepository = userRepository;
        this.collectionEntryRepository = collectionEntryRepository;
        this.likeRepository = likeRepository;
        this.collectionRepository = collectionRepository;
        this.postRepository = postRepository;
        this.photoTaggingService = photoTaggingService;
    }

    // Get paginated list of collections for a specific user
    public Page<CollectionResponse> getCollectionsByUserId(
            Long userId,
            Pageable pageable,
            UserPrincipal principalOrNull
        ) {
            boolean isOwner = principalOrNull != null && principalOrNull.id().equals(userId);

            Page<Collection> collections = isOwner
            ? collectionRepository.findByUser_Id(userId, pageable)
            : collectionRepository.findByUser_IdAndVisibility(userId, CollectionVisibility.PUBLIC, pageable);

    return toCollectionResponsePage(collections);
    }

    // Get paginated list of all collections
    public Page<CollectionResponse> getCollections(
            Pageable pageable
        ) {
            Page<Collection> collections =
                collectionRepository.findByVisibility(CollectionVisibility.PUBLIC, pageable);

        return toCollectionResponsePage(collections);
    }

    // Get detailed information about a specific collection
    public CollectionDetailResponse getCollection(
            Long collectionId,
            Pageable postsPageable,
            UserPrincipal principalOrNull
        ) {

        // Fetch the collection
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found"));

        // Check if the requesting user is the owner of the collection
        boolean isOwner = principalOrNull != null
                && principalOrNull.id().equals(collection.getUser().getId());

        // Enforce visibility rules
        if (collection.getVisibility() == CollectionVisibility.PRIVATE && !isOwner) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found");
        }

        // Fetch post count and cover image
        long postCount = collectionEntryRepository.countByCollection_Id(collection.getId());

        // Get cover image URL from the most recently added post
        String coverImageUrl = collectionEntryRepository
                .findFirstByCollection_IdOrderByAddedAtDesc(collection.getId())
                .map(entry -> entry.getPost().getImageUrl())
                .orElse(null);

        // Fetch paginated posts in the collection
        Page<PostResponse> posts = collectionEntryRepository
                .findByCollection_IdOrderByAddedAtDesc(collection.getId(), postsPageable)
                .map(entry -> toPostResponse(entry.getPost(), principalOrNull));

        // Construct and return the CollectionDetailResponse
        return new CollectionDetailResponse(
                collection.getId(),
                collection.getCollectionName(),
                toAuthor(collection.getUser()),
                collection.getCreatedAt(),
                postCount,
                coverImageUrl,
                PageResponse.from(posts)
            );
    }

    // Create a new collection for the authenticated user
    public CollectionResponse createCollection(CollectionCreateRequest request, UserPrincipal principal) {
        CollectionVisibility visibility =
                request.visibility() != null ? request.visibility() : CollectionVisibility.PUBLIC;

        // principal.id() is the USER id
        User owner = userRepository.findById(principal.id())
                .orElseThrow(() -> new UserNotFoundException(principal.id()));

        Collection collection = new Collection();
        collection.setCollectionName(request.title());
        collection.setVisibility(visibility);
        collection.setUser(owner);

        Collection saved = collectionRepository.save(collection);

        // postCount = 0, coverImageUrl = null initially
        return new CollectionResponse(
                saved.getId(),
                saved.getCollectionName(),
                new AuthorResponse(owner.getId(), owner.getUsername(), owner.getName(), owner.getProfileImageUrl()),
                0L,
                null,
                saved.getVisibility()
            );
    }

    // Update collection's title and visibility (only by owner)
    public CollectionResponse updateCollection(
            Long collectionId,
            CollectionUpdateRequest request,
            UserPrincipal principal
    ) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found"));

        if (!collection.getUser().getId().equals(principal.id())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found");
        }

        collection.setCollectionName(request.title());
        collection.setVisibility(
                request.visibility() != null
                    ? request.visibility()
                    : collection.getVisibility()

        );

        Collection saved = collectionRepository.save(collection);

        long postCount = collectionEntryRepository.countByCollection_Id(saved.getId());

        String coverImageUrl = collectionEntryRepository
                .findFirstByCollection_IdOrderByAddedAtDesc(saved.getId())
                .map(entry -> entry.getPost().getImageUrl())
                .orElse(null);

        return new CollectionResponse(
                saved.getId(),
                saved.getCollectionName(),
                toAuthor(saved.getUser()),
                postCount,
                coverImageUrl,
                saved.getVisibility()
        );
    }

    // Delete a collection (only by owner)
    public void deleteCollection(Long collectionId, UserPrincipal principal) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found"));

        if (!collection.getUser().getId().equals(principal.id())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found");
        }

        collectionEntryRepository.deleteByCollection_Id(collection.getId());
        collectionRepository.delete(collection);
    }


    // Helper methods to convert entities to CollectionResponses
    private AuthorResponse toAuthor(User user) {
        return new AuthorResponse(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getProfileImageUrl()
            );
    }

    // Convert Post entity to PostResponse
    private PostResponse toPostResponse(Post post, UserPrincipal principalOrNull) {
            long likeCount = likeRepository.countByPost_Id(post.getId());

            boolean likedByMe = principalOrNull != null &&
                    likeRepository.existsByUser_IdAndPost_Id(principalOrNull.id(), post.getId());

            LocationResponse locationResponse = null;
            if (post.getLocation() != null) {
                locationResponse = new LocationResponse(
                        post.getLocation().getId(),
                        post.getLocation().getName(),
                        post.getLocation().getSlug(),
                        post.getLocation().getLatitude(),
                        post.getLocation().getLongitude(),
                        post.getLocation().getDescription(),
                        0L // post count for location is not needed here

                );
            }

            List<String> tags = photoTaggingService.getTagsForPost(post.getId());

            return new PostResponse(
                    post.getId(),
                    post.getDescription(),
                    post.getImageUrl(),
                    post.getThumbnailUrl(),
                    post.getCreatedAt(),
                    toAuthor(post.getUser()),
                    locationResponse,
                    likeCount,
                    likedByMe,
                    tags
            );
    }

    // Add a post to a collection (only by owner)
    public void addPostToCollection(Long collectionId, Long postId, UserPrincipal principal) {

        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found"));

        if (!collection.getUser().getId().equals(principal.id())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found");
        }

        if (!collectionEntryRepository.existsByCollection_IdAndPost_Id(collection.getId(), postId)) {

            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

            try {
                collectionEntryRepository.save(new CollectionEntry(collection, post));
            } catch (DataIntegrityViolationException ignored ) {
                // Idempotency: If another request added the same post to
                // the collection after our existence check,
                // we can ignore the exception and treat it as a success.
            }
        }
    }

    public void removePostFromCollection(Long collectionId, Long postId, UserPrincipal principal) {

        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found"));

        // Anti-leak: non-owner gets 404
        if (!collection.getUser().getId().equals(principal.id())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found");
        }

        collectionEntryRepository.deleteByCollection_IdAndPost_Id(collection.getId(), postId);
        // Idempotent: deleting 0 rows is still a success
    }

    // Convert Collection entity to CollectionResponse
    private Page<CollectionResponse> toCollectionResponsePage(Page<Collection> page) {

        // Extract collection IDs
        var ids = page.getContent().stream().map(Collection::getId).toList();
        if (ids.isEmpty()) {
            return page.map(c ->
            new CollectionResponse(
                    c.getId(),
                    c.getCollectionName(),
                    toAuthor(c.getUser()), 0L, null, c.getVisibility()));
        }

        // Fetch counts and cover images in bulk
        var countMap = new java.util.HashMap<Long, Long>();
        for (Object[] r : collectionEntryRepository.countByCollectionIds(ids)) {
            Long collectionId = (Long) r[0];
            Number n = (Number) r[1];
            countMap.put(collectionId, n.longValue());
        }

        // Fetch cover images
        var coverMap = new java.util.HashMap<Long, String>();
        for (Object[] r : collectionEntryRepository.coversByCollectionIds(ids)) {
            Long collectionId = (Long) r[0];
            String imageUrl = (String) r[1];
            if (imageUrl != null) {
                coverMap.putIfAbsent(collectionId, imageUrl);
            }
        }

        // Construct responses
        return page.map(c ->
        new CollectionResponse(
                c.getId(),
                c.getCollectionName(),
                toAuthor(c.getUser()),
                countMap.getOrDefault(c.getId(), 0L),
                coverMap.get(c.getId()),
                c.getVisibility()
            )
        );
    }

}

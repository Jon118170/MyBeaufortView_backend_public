package com.mybeaufortviewproject.mybeaufortview_backend.media;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.ai.AiImageTagProvider;
import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.ai.ImageTagResult;
import com.mybeaufortviewproject.mybeaufortview_backend.post.Post;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostTag;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostTagRepository;

@Service
public class PhotoTaggingService {

    private static final int MAX_TAGS = 8;
    private static final int MAX_TAG_LENGTH = 50;
    private static final Logger log = LoggerFactory.getLogger(PhotoTaggingService.class);

    private final AiImageTagProvider aiImageTagProvider;
    private final PostTagRepository postTagRepository;
    private final PostRepository postRepository;

    public PhotoTaggingService(AiImageTagProvider aiImageTagProvider, PostTagRepository postTagRepository, PostRepository postRepository) {
        this.aiImageTagProvider = aiImageTagProvider;
        this.postTagRepository = postTagRepository;
        this.postRepository = postRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refreshTagsForPost(Post post) {
        if (post == null || post.getId() == null) {
            return;
        }

        String imageUrl = post.getImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            log.info("Skipping tag refresh for postId={} because imageUrl is blank", post.getId());
            postTagRepository.deleteAllByPostId(post.getId());
            return;
        }

        log.info("Refreshing tags for postId={}, imageUrl={}", post.getId(), imageUrl);

        ImageTagResult result;
        try {
            result = aiImageTagProvider.generateTags(imageUrl);
        } catch (Exception ex) {
            log.warn("AI tag generation failed for postId={}", post.getId(), ex);
            return;
        }

        if (result == null) {
            log.warn("AI tag generation returned null for postId={}", post.getId());
            return;
        }

        log.info("Raw AI tag result for postId={}: succes={}, model={}, tags={}",
                post.getId(),
                result.isSuccess(),
                result.getModel(),
                result.getTags());

        List<String> normalizedTags = normalizeTags(result.getTags());

        log.info("Normalized tags for postId={}: {}", post.getId(), normalizedTags);

        saveTagsForPost(post, normalizedTags);

        log.debug("Generated {} tags for postId={} using model={}",
                result.getTags() != null ? result.getTags().size() : 0,
                post.getId(),
                result.getModel());
    }

    public Map<Long, List<String>> getTagsForPostIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }

        return postTagRepository.findByPost_IdIn(postIds).stream()
                .collect(Collectors.groupingBy(
                        postTag -> postTag.getPost().getId(),
                        Collectors.mapping(PostTag::getTag, Collectors.toList())
            ));
    }

    @Transactional(readOnly = true)
    public List<String> getTagsForPost(Long postId) {
        return postTagRepository.findByPost_Id(postId).stream()
                .map(postTag -> postTag.getTag())
                .collect(Collectors.toList());
     }


    private List<String> normalizeTags(List<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return List.of();
        }

        Set<String> normalized = new LinkedHashSet<>();

        for (String raw : rawTags) {
            if (raw == null) {
                continue;
            }

            String cleaned = raw.trim().toLowerCase(Locale.ROOT);

            if (cleaned.isBlank() || (cleaned.length() > MAX_TAG_LENGTH) || cleaned.chars().noneMatch(Character::isLetterOrDigit)) {
                continue;
            }

            normalized.add(cleaned);

            if (normalized.size() >= MAX_TAGS) {
                break;
                }
            }

            return List.copyOf(normalized);
        }

    private void saveTagsForPost(Post post, List<String> normalizedTags) {
        postTagRepository.deleteAllByPostId(post.getId());
        postTagRepository.flush();

        if (normalizedTags.isEmpty()) {
            return;
        }

        // Use a managed proxy so Hibernate can resolve the FK without a detached-entity error
        Post managedPost = postRepository.getReferenceById(post.getId());

        List<PostTag> tagsToSave = normalizedTags.stream().map(tag -> {
                PostTag postTag = new PostTag();
                postTag.setPost(managedPost);
                postTag.setTag(tag);
                return postTag;
            }).toList();

        postTagRepository.saveAll(tagsToSave);
    }
}

package com.mybeaufortviewproject.mybeaufortview_backend.media;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.ai.AiImageTagProvider;
import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.ai.ImageTagResult;
import com.mybeaufortviewproject.mybeaufortview_backend.post.Post;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostTag;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostTagRepository;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class PhotoTaggingServiceTest {

    @Mock
    private AiImageTagProvider aiImageTagProvider;

    @Mock
    private PostTagRepository postTagRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PhotoTaggingService photoTaggingService;

    @Captor
    private ArgumentCaptor<List<PostTag>> postTagListCaptor;

    private Post post;

    @BeforeEach
    public void setUp() {
        post = new Post();
        post.setId(1L);
        post.setImageUrl("http://example.com/image.jpg");
    }

    @Test
    public void refreshTagsForPost_savesNormalizedDeduplicatedTags() {
        when(aiImageTagProvider.generateTags(anyString()))
        .thenReturn(new ImageTagResult(
                List.of("beach", "sunset", " beach ", "SUNSET"),
                "openai-vision",
                true
        ));

        photoTaggingService.refreshTagsForPost(post);

        verify(postTagRepository).deleteAllByPostId(1L);
        verify(postTagRepository).flush();
        verify(postTagRepository).saveAll(postTagListCaptor.capture());

        List<PostTag> savedTags = postTagListCaptor.getValue();

        assertEquals(List.of("beach", "sunset"),
                savedTags.stream().map(PostTag::getTag).toList());
    }


    @Test
    public void refreshTagsForPost_blankImageUrl_deletesExistingTagsAndSkipsProvider() {
        post.setImageUrl("    ");

        photoTaggingService.refreshTagsForPost(post);

        verify(postTagRepository).deleteAllByPostId(1L);
        verify(aiImageTagProvider, never()).generateTags(anyString());
        verify(postTagRepository, never()).saveAll(anyList());
    }

    @Test
    public void refreshTagsForPost_nullPost_doesNothing() {
        photoTaggingService.refreshTagsForPost(null);

        verifyNoInteractions(aiImageTagProvider, postTagRepository);
    }

    @Test
    public void refreshTagsForPost_postWithoutId_doesNothing() {
        Post unsavedPost = new Post();
        unsavedPost.setImageUrl("http://example.com/image.jpg");

        photoTaggingService.refreshTagsForPost(unsavedPost);

        verifyNoInteractions(aiImageTagProvider, postTagRepository);
    }

    @Test
    public void refreshTagsForPost_providerReturnsEmpty_deletesOldTagsAndDoesNotSaveNewOnes() {
        when(aiImageTagProvider.generateTags(post.getImageUrl()))
            .thenReturn(new ImageTagResult(List.of(), "openai-vision", true));

        photoTaggingService.refreshTagsForPost(post);

        verify(postTagRepository).deleteAllByPostId(1L);
        verify(postTagRepository, never()).saveAll(anyList());
    }

    @Test
    public void refreshTagsForPost_providerFailure_doesNotThrow() {
        when(aiImageTagProvider.generateTags(post.getImageUrl()))
            .thenThrow(new RuntimeException("AI service error"));

        assertDoesNotThrow(() -> photoTaggingService.refreshTagsForPost(post));

        verify(postTagRepository, never()).saveAll(anyList());
    }

    @Test
    public void getTagsForPost_returnsTagStrings() {
        PostTag tag1 = new PostTag();
        tag1.setTag("sunset");

        PostTag tag2 = new PostTag();
        tag2.setTag("beach");

        when(postTagRepository.findByPost_Id(1L)).thenReturn(List.of(tag1, tag2));

        List<String> result = photoTaggingService.getTagsForPost(1L);

        assertEquals(List.of("sunset", "beach"), result);
    }

    @Test
    public void getTagsForPostIds_groupsTagsByPostId() {
        Post post1 = new Post();
        post1.setId(1L);

        Post post2 = new Post();
        post2.setId(2L);

        PostTag tag1 = new PostTag();
        tag1.setPost(post1);
        tag1.setTag("sunset");

        PostTag tag2 = new PostTag();
        tag2.setPost(post1);
        tag2.setTag("beach");

        PostTag tag3 = new PostTag();
        tag3.setPost(post2);
        tag3.setTag("mountains");

        when(postTagRepository.findByPost_IdIn(List.of(1L, 2L)))
            .thenReturn(List.of(tag1, tag2, tag3));

        Map<Long, List<String>> result = photoTaggingService.getTagsForPostIds(List.of(1L, 2L));

        assertEquals(List.of("sunset", "beach"), result.get(1L));
        assertEquals(List.of("mountains"), result.get(2L));
    }

}

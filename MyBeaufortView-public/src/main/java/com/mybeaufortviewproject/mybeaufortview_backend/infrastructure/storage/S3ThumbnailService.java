package com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mybeaufortviewproject.mybeaufortview_backend.common.config.AppProperties;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.StorageException;
import com.mybeaufortviewproject.mybeaufortview_backend.post.Post;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostRepository;

import net.coobird.thumbnailator.Thumbnails;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@Transactional
@ConditionalOnProperty(prefix = "aws.s3", name = "enabled", havingValue = "true")
public class S3ThumbnailService implements ThumbnailService {

    private final S3Client s3Client;
    private final PostRepository postRepository;
    private final AppProperties appProperties;

    public S3ThumbnailService(S3Client s3Client, PostRepository postRepository, AppProperties appProperties) {
        this.s3Client = s3Client;
        this.postRepository = postRepository;
        this.appProperties = appProperties;
    }


    @Override
    public void generateThumbnailForPost(Post post) {
        if (post == null || post.getId() == null) {
            return;
        }

        String imageUrl = post.getImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        String bucketName = appProperties.getAws().getS3().getBucketName();
        String region = appProperties.getAws().getS3().getRegion();

        String originalKey = extractKeyFromUrl(imageUrl);
        String thumbnailKey = buildThumbnailKey(post.getId(), originalKey);

        try (
                InputStream originalStream = s3Client.getObject(
                        GetObjectRequest.builder()
                                .bucket(bucketName)
                                .key(originalKey)
                                .build()
                );
                ByteArrayOutputStream thumbnailOutput = new ByteArrayOutputStream()
        ) {
            Thumbnails.of(originalStream)
                    .size(200, 200)
                    .outputFormat("jpg")
                    .toOutputStream(thumbnailOutput);

            byte[] thumbnailBytes = thumbnailOutput.toByteArray();

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(thumbnailKey)
                            .contentType("image/jpeg")
                            .build(),
                    RequestBody.fromInputStream(
                            new ByteArrayInputStream(thumbnailBytes),
                            thumbnailBytes.length
              )
            );

            String thumbnailUrl = "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + thumbnailKey;

            post.setThumbnailUrl(thumbnailUrl);
            postRepository.save(post);
            } catch (Exception e) {
                throw new StorageException("Failed to generate thumbnail for post ID " + post.getId(), e);
            }
        }

    private String extractKeyFromUrl(String imageUrl) {
        URI uri = URI.create(imageUrl);
        String path = uri.getPath();
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private String buildThumbnailKey(Long postId, String originalKey) {
        String extension = "jpg";
        int lastDot = originalKey.lastIndexOf('.');
        if (lastDot > 0 && lastDot < originalKey.length() - 1) {
            extension = originalKey.substring(lastDot + 1).toLowerCase();
        }

        return "thumbnails/post-" + postId + "." + extension;
    }

}

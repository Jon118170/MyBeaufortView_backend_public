package com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.storage;

import java.net.URL;
import java.time.Duration;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.mybeaufortviewproject.mybeaufortview_backend.common.config.AppProperties;
import com.mybeaufortviewproject.mybeaufortview_backend.media.dto.PresignUploadResponse;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@ConditionalOnProperty(prefix = "aws.s3", name = "enabled", havingValue = "true")
public class S3Service {

    private final S3Presigner presigner;

    private final AppProperties appProperties;

    public S3Service(S3Presigner presigner, AppProperties appProperties) {
        this.presigner = presigner;
        this.appProperties = appProperties;
    }

    public PresignUploadResponse createPresignedPutUrl(String contentType, String fileExtension) {
        String safeExt = (fileExtension == null || fileExtension.isBlank())
                ? "jpg"
                : fileExtension.toLowerCase();

        String key = "uploads/" + UUID.randomUUID() + "." + safeExt;

        String bucketName = appProperties.getAws().getS3().getBucketName();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObjectRequest)
                .build();

        URL uploadUrl = presigner.presignPutObject(presignRequest).url();

        String region = appProperties.getAws().getS3().getRegion();

        // Virtual-hosted–style URL
        String fileUrl = "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;

        return new PresignUploadResponse(uploadUrl.toString(), fileUrl, key);
    }

}

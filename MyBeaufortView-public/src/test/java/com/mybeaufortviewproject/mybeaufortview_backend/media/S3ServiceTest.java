package com.mybeaufortviewproject.mybeaufortview_backend.media;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

import com.mybeaufortviewproject.mybeaufortview_backend.common.config.AppProperties;
import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.storage.S3Service;
import com.mybeaufortviewproject.mybeaufortview_backend.media.dto.PresignUploadResponse;

import jakarta.transaction.Transactional;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;


@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = true)
@Transactional
public class S3ServiceTest {

    @Test
    public void createPresignedPutUrl_buildsExpectedResponse() throws Exception {

        // Mock the S3Presigner and its response
        S3Presigner presigner = mock(S3Presigner.class);
        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);

        when(presigned.url()).thenReturn(URI.create("https://example.com/presigned").toURL());
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class)))
        .thenReturn(presigned);

        // Build app properties
        AppProperties props = buildProps("test-bucket", "us-east-1");

        // Create the service with the mocked presigner
        S3Service service = new S3Service(presigner, props);

        PresignUploadResponse resp =
                service.createPresignedPutUrl("image/jpeg", "jpg");

        assertNotNull(resp.getUploadUrl());
        assertTrue(resp.getFileUrl().contains("test-bucket"));
        assertTrue(resp.getKey().startsWith("uploads/"));
    }

    private static AppProperties buildProps(String bucket, String region) {
        AppProperties props = new AppProperties();
        props.getAws().getS3().setBucketName(bucket);
        props.getAws().getS3().setRegion(region);
        return props;
    }


}

package com.mybeaufortviewproject.mybeaufortview_backend.media;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.storage.S3Service;
import com.mybeaufortviewproject.mybeaufortview_backend.media.dto.PresignUploadRequest;
import com.mybeaufortviewproject.mybeaufortview_backend.media.dto.PresignUploadResponse;

@RestController
@RequestMapping("/api/uploads")
@ConditionalOnProperty(prefix = "aws.s3", name = "enabled", havingValue = "true")
public class S3Controller {

    private final S3Service s3Service;

    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping("/presign")
    public ResponseEntity<PresignUploadResponse> presign(@RequestBody PresignUploadRequest request) {
        PresignUploadResponse response = s3Service.createPresignedPutUrl(
                request.getContentType(),
                request.getFileExtension()
        );
        return ResponseEntity.ok(response);
    }
}

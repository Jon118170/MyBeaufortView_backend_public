package com.mybeaufortviewproject.mybeaufortview_backend.media.dto;

import jakarta.validation.constraints.NotBlank;

public class PresignUploadRequest {

    @NotBlank
    private String contentType;        // example: "image/png"

    @NotBlank
    private String fileextension;   // example: ".png"

    public PresignUploadRequest() {}

    public PresignUploadRequest(String contentType, String fileExtension) {
        this.contentType = contentType;
        this.fileextension = fileExtension;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFileExtension() {
        return fileextension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileextension = fileExtension;
    }

}

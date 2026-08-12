package com.mybeaufortviewproject.mybeaufortview_backend.media.dto;

public class PresignUploadResponse {
    private String uploadUrl; // presigned URL for uploading
    private String fileUrl;   // public URL to access the uploaded file
    private String fileKey;   // S3 object key: debugging

    public PresignUploadResponse() {}

    public PresignUploadResponse(String uploadUrl, String fileUrl, String fileKey) {
        this.uploadUrl = uploadUrl;
        this.fileUrl = fileUrl;
        this.fileKey = fileKey;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getKey() {
        return fileKey;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public void setKey(String fileKey) {
        this.fileKey = fileKey;
    }
}

package com.mybeaufortviewproject.mybeaufortview_backend.commission.dto;

public class CommissionRequestCreate {

    private Long photographerId;
    private Long postId;
    private String message;

    public Long getPhotographerId() {
        return photographerId;
    }

    public void setPhotographerId(Long photographerId) {
        this.photographerId = photographerId;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

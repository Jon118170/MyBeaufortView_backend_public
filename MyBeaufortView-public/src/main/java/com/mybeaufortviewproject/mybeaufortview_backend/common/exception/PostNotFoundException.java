package com.mybeaufortviewproject.mybeaufortview_backend.common.exception;

import org.springframework.http.HttpStatus;

public class PostNotFoundException extends AppException {

    private static final long serialVersionUID = 1L;

    public PostNotFoundException(Long id) {
        super(
                ErrorCode.POST_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Could not find post " + id
        );
    }

    public PostNotFoundException(String post) {
        super(
                ErrorCode.POST_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Could not find post " + post
        );
    }

    public PostNotFoundException() {
        super(
                ErrorCode.POST_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Post not found"
        );
    }

}

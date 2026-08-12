package com.mybeaufortviewproject.mybeaufortview_backend.common.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends AppException {

    private static final long serialVersionUID = 1L;

    public UserNotFoundException(Long id) {
        super(
            ErrorCode.USER_NOT_FOUND,
            HttpStatus.NOT_FOUND,
            "User not found with id: " + id
        );
    }

    public UserNotFoundException(String email) {
        super(
            ErrorCode.USER_NOT_FOUND,
            HttpStatus.NOT_FOUND,
            "User not found with email: " + email
        );
    }

}

package com.mybeaufortviewproject.mybeaufortview_backend.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends AppException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException(String message ) {
        super(
                ErrorCode.ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                message
        );
    }
}

package com.mybeaufortviewproject.mybeaufortview_backend.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends AppException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedException(ErrorCode code, String message) {
        super(
                code,
                HttpStatus.UNAUTHORIZED,
                message
        );
    }
}

package com.mybeaufortviewproject.mybeaufortview_backend.common.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends AppException {

    private static final long serialVersionUID = 1L;

    public BadRequestException(String message) {
        super(
                ErrorCode.BAD_REQUEST,
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}

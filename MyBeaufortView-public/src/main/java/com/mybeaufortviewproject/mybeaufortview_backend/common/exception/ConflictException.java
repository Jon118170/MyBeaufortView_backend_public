package com.mybeaufortviewproject.mybeaufortview_backend.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends AppException {

    private static final long serialVersionUID = 1L;

    public ConflictException(ErrorCode code, String message) {
        super(
                code,
                HttpStatus.CONFLICT,
                message
        );
    }

}

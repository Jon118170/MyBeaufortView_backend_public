package com.mybeaufortviewproject.mybeaufortview_backend.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends AppException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(ErrorCode code, String message) {
        super(code,
              HttpStatus.NOT_FOUND,
              message
        );
    }
}

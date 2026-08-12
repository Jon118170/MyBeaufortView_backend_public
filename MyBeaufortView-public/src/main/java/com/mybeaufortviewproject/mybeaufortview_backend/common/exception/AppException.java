package com.mybeaufortviewproject.mybeaufortview_backend.common.exception;

import org.springframework.http.HttpStatus;

public abstract class AppException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;
    private final HttpStatus status;

    protected AppException(
            ErrorCode code,
            HttpStatus status,
            String message
    ) {
        super(message);
        this.code = code;
        this.status = status;
    }

    protected AppException(
            ErrorCode code,
            HttpStatus status,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    public ErrorCode getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

}

package com.mybeaufortviewproject.mybeaufortview_backend.common.exception;

import org.springframework.http.HttpStatus;

public class StorageException extends AppException {

    private static final long serialVersionUID = 1L;

    public StorageException(String message, Throwable cause) {
        super(
                ErrorCode.STORAGE_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR,
                message,
                cause
        );
    }
}

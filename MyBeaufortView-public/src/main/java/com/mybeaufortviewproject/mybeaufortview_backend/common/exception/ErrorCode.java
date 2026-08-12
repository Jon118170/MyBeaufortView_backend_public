package com.mybeaufortviewproject.mybeaufortview_backend.common.exception;

public enum ErrorCode {

    // Authentication / Authorization
    AUTH_REQUIRED,
    AUTH_INVALID_TOKEN,
    AUTH_EXPIRED_TOKEN,
    ACCESS_DENIED,

    // Validation
    VALIDATION_FAILED,
    BAD_REQUEST,

    // Resource
    USER_NOT_FOUND,
    POST_NOT_FOUND,
    COLLECTION_NOT_FOUND,
    LOCATION_NOT_FOUND,
    MEDIA_JOB_NOT_FOUND,
    NOTIFICATION_NOT_FOUND,
    COMMISSION_REQUEST_NOT_FOUND,

    // Conflicts
    DUPLICATE_RESOURCE,
    COLLECTION_ALREADY_EXISTS,
    LIKE_ALREADY_EXISTS,

    // Infrastructure
    STORAGE_ERROR,
    EXTERNAL_SERVICE_ERROR,
    RATE_LIMITED,

    // Generic
    INTERNAL_ERROR

}

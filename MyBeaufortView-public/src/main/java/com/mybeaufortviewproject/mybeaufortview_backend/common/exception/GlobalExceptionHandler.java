package com.mybeaufortviewproject.mybeaufortview_backend.common.exception;

import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Handle custom application exceptions that extend AppException
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiError> handleAppException(
            AppException ex,
            HttpServletRequest request
    ) {
        log.warn(
                "Application error: code={}, status={}, path={}, message={}",
                ex.getCode(),
                ex.getStatus().value(),
                request.getRequestURI(),
                ex.getMessage()
        );

        return  buildResponse(
                ex.getStatus(),
                ex.getStatus().getReasonPhrase(),
                ex.getCode(),
                ex.getMessage(),
                request
        );
    }

    // Handle validation errors from @Valid annotations in controllers
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");

        log.warn(
                "Validation error: path={}, message={}",
                request.getRequestURI(),
                message
        );

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ErrorCode.VALIDATION_FAILED,
                message,
                request
        );
    }

    // Handle Spring Security's AccessDeniedException for unauthorized access attempts
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        log.warn(
                "Access denied: path={}, message={}",
                request.getRequestURI(),
                ex.getMessage()
        );

        return buildResponse(
                HttpStatus.FORBIDDEN,
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ErrorCode.ACCESS_DENIED,
                "Access denied",
                request
        );
    }

    // Handle Spring's ResponseStatusException for cases where controllers throw it directly
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {

        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

        log.warn(
                "Response status error: status={}, path={}, message={}",
                status.value(),
                request.getRequestURI(),
                ex.getReason()
        );

        return buildResponse(
                status,
                status.getReasonPhrase(),
                mapStatusToCode(status),
                Optional.ofNullable(ex.getReason()).orElse(status.getReasonPhrase()),
                request
        );
    }

    // Catch-all for any other exceptions that weren't handled above
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {

        log.error(
                "Unhandled exception: path={}, traceId={}",
                request.getRequestURI(),
                resolveTraceId(request),
                ex
        );

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred",
                request
        );
    }

    // Helper method to build consistent API error responses
    private ResponseEntity<ApiError> buildResponse(
            HttpStatus status,
            String error,
            ErrorCode code,
            String message,
            HttpServletRequest request
    ) {
        ApiError apiError = new ApiError(
                Instant.now(),
                status.value(),
                error,
                code.name(),
                message,
                request.getRequestURI(),
                resolveTraceId(request)
        );

        return new ResponseEntity<>(apiError, status);
    }

    // Extract trace ID from header or generate a new one if not present
    private String resolveTraceId(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("X-Correlation-Id"))
                .orElseGet(() -> UUID.randomUUID().toString());
    }

    // Map common HTTP statuses to application error codes
    private ErrorCode mapStatusToCode(HttpStatus status) {
        return switch (status) {
        case BAD_REQUEST -> ErrorCode.BAD_REQUEST;
        case UNAUTHORIZED -> ErrorCode.AUTH_REQUIRED;
        case FORBIDDEN -> ErrorCode.ACCESS_DENIED;
        case NOT_FOUND -> ErrorCode.BAD_REQUEST;
        case CONFLICT -> ErrorCode.DUPLICATE_RESOURCE;
        case TOO_MANY_REQUESTS -> ErrorCode.RATE_LIMITED;
        default -> ErrorCode.INTERNAL_ERROR;
        };
    }

}

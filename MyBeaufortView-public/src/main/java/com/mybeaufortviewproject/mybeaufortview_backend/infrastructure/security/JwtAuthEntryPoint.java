package com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.ApiError;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.ErrorCode;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // This method is called when an unauthenticated user tries to access a protected resource
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        String traceId = Optional
                .ofNullable(request.getHeader("X-Correlation-Id"))
                .filter(id -> !id.isEmpty())
                .orElseGet(() -> UUID.randomUUID().toString());

        ErrorCode code = (ErrorCode) request.getAttribute(
                JwtErrorAttributes.JWT_ERROR_CODE
        );

        // If the filter didn't set a specific error code, default to AUTH_REQUIRED
        if (code == null) {
            code = ErrorCode.AUTH_REQUIRED;
        }

        String message = (String) request.getAttribute(
                JwtErrorAttributes.JWT_ERROR_MESSAGE
        );

        // If the filter didn't set a specific message, use the exception's message
        if (message == null || message.isBlank()) {
            message = authException.getMessage();
        }

        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                code.name(),
                message,
                request.getRequestURI(),
                traceId
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("X-Correlation-Id", traceId);

        objectMapper.writeValue(response.getOutputStream(), error);
    }
}

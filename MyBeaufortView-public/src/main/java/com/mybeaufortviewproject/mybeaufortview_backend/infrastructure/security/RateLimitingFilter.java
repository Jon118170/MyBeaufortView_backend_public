package com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security;

import java.io.IOException;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.ratelimit.RateLimitService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Profile("!test") // Disable in tests to avoid interference
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    public RateLimitingFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only rate limit selected endpoints
        if (shouldRateLimit(path, request.getMethod())) {

            String key = getKey(request);

            if (!rateLimitService.isAllowed(key)) {
                response.setStatus(429);
                response.setContentType("applicaiton/json");

                response.getWriter().write("""
                    {
                        "status": 429,
                        "error": "Too Many Requests",
                        "message": "Rate limit exceeded"
                    }
                """);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldRateLimit(String path, String method) {
        return path.startsWith("/api/commissions")
                || path.contains("/like")
                || path.startsWith("/api/auth")
                || path.startsWith("/api/notifications/stream");
    }

    private String getKey(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();
        return ip + ":" + path;
    }


}

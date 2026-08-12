package com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security;

import java.io.IOException;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.ErrorCode;
import com.mybeaufortviewproject.mybeaufortview_backend.user.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

      private final JwtUtil jwtUtil;
      private final UserRepository userRepository;

       // Constructor injection
       public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
           this.jwtUtil = jwtUtil;
           this.userRepository = userRepository;
       }

       // Core filtering logic
       @Override
       protected void doFilterInternal(HttpServletRequest request,
                                          HttpServletResponse response,
                                          FilterChain filterChain) throws ServletException, IOException {

           var existingAuth = SecurityContextHolder.getContext().getAuthentication();
            if (existingAuth != null && existingAuth.isAuthenticated()
                && !(existingAuth instanceof AnonymousAuthenticationToken)) {
                 // If already authenticated, skip processing
                filterChain.doFilter(request, response);
                return;
           }

           String token = extractToken(request);

           if (token != null) {
               try {
                   // Validate token
                   Claims claims = jwtUtil.validateToken(token);

                   Long userId = claims.get("id", Long.class);
                   String role = claims.get("role", String.class);
                   String email = claims.getSubject();

                   if (userId == null || role == null || role.isBlank()) {
                       SecurityContextHolder.clearContext();
                       filterChain.doFilter(request, response);
                       return;
                   }

                   boolean userExists = userRepository.existsById(userId);
                    if (!userExists) {
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }

                    UserPrincipal principal = new UserPrincipal(userId, email, role);

                   // Set authentication in security context
                   UsernamePasswordAuthenticationToken authentication =
                           new UsernamePasswordAuthenticationToken(
                                   principal, // set the full user object
                                   null,
                                   principal.getAuthorities()
                            );
                   authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                   SecurityContextHolder.getContext().setAuthentication(authentication);

               // Handle token validation errors
               } catch (ExpiredJwtException e) {
                    SecurityContextHolder.clearContext();

                    request.setAttribute(
                            JwtErrorAttributes.JWT_ERROR_CODE,
                            ErrorCode.AUTH_EXPIRED_TOKEN
                    );

                    request.setAttribute(
                            JwtErrorAttributes.JWT_ERROR_MESSAGE,
                            "JWT token has expired"
                    );

                // Catch any other JWT-related exceptions (malformed, unsupported, signature issues, etc.)
                } catch (JwtException | IllegalArgumentException e) {
                    SecurityContextHolder.clearContext();

                    request.setAttribute(
                            JwtErrorAttributes.JWT_ERROR_CODE,
                            ErrorCode.AUTH_INVALID_TOKEN
                    );

                    request.setAttribute(
                            JwtErrorAttributes.JWT_ERROR_MESSAGE,
                            "JWT token is invalid"
                    );
                }
           }

           // Continue filter chain if token is valid or not present
           filterChain.doFilter(request, response);
       }

       // Helper method to extract the JWT token from the Authorization header or query param (SSE)
       private String extractToken(HttpServletRequest request) {
           String authHeader = request.getHeader("Authorization");
           if (authHeader != null && authHeader.startsWith("Bearer ")) {
               return authHeader.substring(7);
           }
           // EventSource cannot send headers, so SSE connections pass the token as a query param
           if (request.getRequestURI().endsWith("/stream")) {
               return request.getParameter("token");
           }
           return null;
       }

       // Exclude Swagger and API docs paths from JWT authentication
       @Override
        protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
            String path = request.getRequestURI();

            return path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.equals("/webjars");
        }
    }

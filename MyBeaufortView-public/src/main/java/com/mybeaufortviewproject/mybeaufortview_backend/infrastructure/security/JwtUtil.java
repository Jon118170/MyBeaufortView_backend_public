package com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security;

import java.util.Date;
import java.util.logging.Logger;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.mybeaufortviewproject.mybeaufortview_backend.common.config.AppProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Component
public class JwtUtil {

    private static final long JWT_EXPIRATION_MS = 86400000L; // 24 hours

    private static final Logger LOGGER = Logger.getLogger(JwtUtil.class.getName());


    private final AppProperties appProperties;


    private String jwtSecret;
    private SecretKey signingKey;

    public JwtUtil(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void init() {
        this.jwtSecret = appProperties.getJwt().getSecretKey();

        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT secret key is not configured properly.");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT secret key is not valid Base64.", e);
        }

        if (keyBytes.length < 32) { // 256 bits for HS256
            throw new IllegalStateException(
                    "JWT secret key is too short. It must be at least 256 bits (32 bytes) long.");
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private SecretKey getSigningKey() {

        // signingKey is initialized in @PostConstruct; this is just a safety check
        if (signingKey == null) {
            throw new IllegalStateException("Signing key has not been initialized.");
        }
        return signingKey;
    }

    // Generate JWT token with email, role, and id claims
    public String generateToken(String email, String role, Long id) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("id", id)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION_MS))
                .signWith(getSigningKey()) // Sign the token with the secret key
                .compact();
    }

    // Validate the JWT token and return the claims
    public Claims validateToken(String token) {
        try {
            // Parse and validate the JWT token
            Claims parser = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return parser;

        } catch (JwtException | IllegalArgumentException e) {
            LOGGER.warning("Jwt validation failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            throw e; // Rethrow exception for handling upstream
        }
    }

    // Extract the username (email) from the JWT token
    public String extractUsername(String token) {
        return validateToken(token).getSubject();
    }

    // Check to see if the token had expired
    public boolean isTokenExpired(String token) {
        Date expiration = validateToken(token).getExpiration();
        return expiration.before(new Date());
    }

}

package com.mybeaufortviewproject.mybeaufortview_backend.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.JwtUtil;
import com.mybeaufortviewproject.mybeaufortview_backend.user.Role;
import com.mybeaufortviewproject.mybeaufortview_backend.user.User;
import com.mybeaufortviewproject.mybeaufortview_backend.user.UserRepository;

public final class SecurityTestUtils {

    private SecurityTestUtils() {}


    // Add Bearer token to request
    public static RequestPostProcessor bearer(String token) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    // Seed a user into the repository
    public static User seedUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String email,
            Role role
    ) {
        String uniqueSuffix = Long.toString(System.nanoTime()); // or UUID
        User u = new User(
                "testuser_" + role.name().toLowerCase() + "_" + uniqueSuffix,
                "Test " + role.name(),
                email,
                passwordEncoder.encode("password123"),
                role
        );
        return userRepository.save(u);
    }

    public static String tokenFor(User user, JwtUtil jwtUtil) {

        // Generate JWT token for the user
        return jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getId()
        );
    }
}

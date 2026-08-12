package com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.BadRequestException;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.ConflictException;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.ErrorCode;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.UnauthorizedException;
import com.mybeaufortviewproject.mybeaufortview_backend.user.Role;
import com.mybeaufortviewproject.mybeaufortview_backend.user.User;
import com.mybeaufortviewproject.mybeaufortview_backend.user.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    /* Registers a new user by encoding their password and saving them to the repository */
    public String registerUser(String name, String username, String email, String password, String role) {
        // Check if user with email already exists
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "User with email " + email + " already exists"
            );
        }

        // Check if user with username already exists
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "User with username " + username + " already exists"
            );
        }

        // Encode password
        String encodedPassword = passwordEncoder.encode(password);

        // Validate and set role
        Role userRole;
        try {
            String normalizedRole = role.trim().toUpperCase().replace(" ", "_");
            userRole = Role.valueOf(normalizedRole);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + role);
        }

        // Create new user and save to repository
        User newUser = new User(username, name, email, encodedPassword, userRole);

        // Save user to repository
        userRepository.save(newUser);

        // Return success message
        return "User registered successfully";

    }

    public User getCurrentUser() {

        // Get the current authenticated principal
        UserPrincipal principal = getCurrentPrincipal();
        return userRepository.findById(principal.id())
                .orElseThrow(() -> new UnauthorizedException(
                        ErrorCode.AUTH_REQUIRED,
                        "Authenticated user not found"
        ));
    }

    public UserPrincipal getCurrentPrincipal() {

        // Get the current authentication object from the security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Validate that the user is authenticated and not anonymous
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException(
                    ErrorCode.AUTH_REQUIRED,
                    "User not authenticated"
            );
        }

        // Ensure the principal is of type UserPrincipal
        Object principal = auth.getPrincipal();

        // Validate that the principal is an instance of UserPrincipal
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            throw new IllegalStateException("Authenticated principal is not of type UserPrincipal");
        }

        return userPrincipal;
    }

    public LoginResponse loginUser(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new UnauthorizedException(
                            ErrorCode.AUTH_REQUIRED,
                            "User not found: " + loginRequest.getEmail()
                    ));

            String token = jwtUtil.generateToken(
                    user.getEmail(),
                    user.getRole().name(),
                    user.getId()
            );

            return new LoginResponse(
                    token,
                    user.getEmail(),
                    user.getRole().name(),
                    user.getUsername()
            );

        } catch (AuthenticationException e) {
            throw new UnauthorizedException(
                    ErrorCode.AUTH_INVALID_TOKEN,
                    "Invalid credentials");
        }

    }

    public String registerUser(AuthRequest authRequest) {
        String role = "PRIVILEGED_USER";

        return registerUser(
                authRequest.getName(),
                authRequest.getUsername(),
                authRequest.getEmail(),
                authRequest.getPassword(),
                role
        );
    }

}

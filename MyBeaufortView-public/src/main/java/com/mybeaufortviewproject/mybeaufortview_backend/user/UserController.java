package com.mybeaufortviewproject.mybeaufortview_backend.user;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mybeaufortviewproject.mybeaufortview_backend.common.dto.PageResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.UserPrincipal;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.PostResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.user.dto.UserUpdateRequest;
import com.mybeaufortviewproject.mybeaufortview_backend.user.dto.UserProfileResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.user.dto.UserProfileUpdateRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final UserProfileService userProfileService;

    public UserController(
            UserService userService, UserProfileService userProfileService
    ) {
        this.userService = userService;
        this.userProfileService = userProfileService;
    }

    // Create a new user
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(
            value = "/user",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<User> newUser(
            @RequestBody UserUpdateRequest request
    ) {
        User savedUser = userService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedUser);
    }

    // Retrieve all users sorted by id in ascending order
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // Retrieve a user by their ID
    @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(#id, authentication)")
    @GetMapping("/user/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping("/users/{userId}/profile")
    public UserProfileResponse getUserProfile(@PathVariable Long userId) {
        return userProfileService.getProfile(userId);
    }

    @GetMapping("/users/{userId}/posts")
    public PageResponse<PostResponse> getUserPosts(
            @PathVariable Long userId,
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return PageResponse.from(
                userProfileService.getUserPosts(userId, pageable, principal)
        );
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/users/me/profile")
    public UserProfileResponse updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid UserProfileUpdateRequest profileUpdateRequest
    ) {
        return userProfileService.updateMyProfile(principal, profileUpdateRequest);
    }

    // Update a post by id
    @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(#id, authentication)")
    @PutMapping(value = "/user/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public User updateUser(
            @RequestBody UserUpdateRequest request,
            @PathVariable Long id
    ) {
        return userService.updateUser(id, request);
    }

    // Delete a user by id
    @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(#id, authentication)")
    @DeleteMapping("/user/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

}

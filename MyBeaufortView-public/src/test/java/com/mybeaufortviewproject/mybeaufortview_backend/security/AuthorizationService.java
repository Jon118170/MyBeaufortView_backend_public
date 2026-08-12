package com.mybeaufortviewproject.mybeaufortview_backend.security;

import org.springframework.security.core.Authentication;

// AuthorizationService confirms whether the user can edit their own
// accounts, whether the post belong to the authenticated user,
// and whether the collection belong to the authenticated user.
// Without it, you will have scattered ownership checks across
// controllers and services. Thus, easy to miss an endpoint.

import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.UserPrincipal;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.user.User;
import com.mybeaufortviewproject.mybeaufortview_backend.user.UserRepository;

@Component("authz")
@ActiveProfiles("test")
public class AuthorizationService {

    private final UserRepository users;
    private final PostRepository posts;

    public AuthorizationService(
            UserRepository users,
            PostRepository posts
    ) {
        this.users = users;
        this.posts = posts;
    }

    // Reads userId from Authentication. Adjust based on how you store user info.
    private Long currentUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }

        Object principal = auth.getPrincipal();

        // Your standard setup with UserPrincipal
        if (principal instanceof UserPrincipal up) {
            return up.id();
        }

        // Your JwtAuthenticationFilter sets principal = User entity
        if (principal instanceof User u) {
            return u.getId();
        }

        // Fallback: assume principal is email (String)
        if (principal instanceof String s) {
            if ("anonymousUser".equals(s)) {
                return null;
            }
            return users.findByEmail(s).map(User::getId).orElse(null);
        }

        // Fallback: use auth name as email
        String email = auth.getName();
        return users.findByEmail(email).map(u -> u.getId()).orElse(null);
    }

    public boolean isSelf(Long userId, Authentication auth) {
        Long me = currentUserId(auth);
        return me != null && me.equals(userId);
    }

    public boolean ownsPost(Long postId, Authentication auth) {
        Long me = currentUserId(auth);
        if (me == null) {
            return false;
        }
        return posts.findById(postId)
                .map(p -> p.getUser() != null && me.equals(p.getUser().getId()))
                .orElse(false);
    }

    // TODO: Implement collection ownership when Collection has a User owner.
    // Deferred to feature/collection-ownership
    public boolean ownsCollection(Long collectionId, Authentication auth) {
        return false;
    }
}

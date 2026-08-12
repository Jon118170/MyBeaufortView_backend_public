package com.mybeaufortviewproject.mybeaufortview_backend.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.JwtUtil;
import com.mybeaufortviewproject.mybeaufortview_backend.security.SecurityTestUtils;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@Transactional
public class UserProfileSecurityIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User owner;
    private String ownerToken;

    @BeforeEach
    public void setup() {
        owner = SecurityTestUtils.seedUser(
                userRepository,
                passwordEncoder,
                "owner@example.com",
                Role.PRIVILEGED_USER
        );

        ownerToken = SecurityTestUtils.tokenFor(owner, jwtUtil);
    }

    // -------------------------
    // PUT /api/users/me/profile
    // -------------------------

    @Test
    public void noToken_updateProfile_shouldReturn401() throws Exception {
        String payload = """
        { "name": "Updated Name", "bio": "Bio", "profileImageUrl": "http://example.com/p.png" }
        """;

        mockMvc.perform(put("/api/users/me/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void authenticated_updateProfile_shouldReturn200() throws Exception {
        String payload = """
                { "name": "Updated Name", "bio": "New Bio", "profileImageUrl": "http://example.com/p.png" }
                """;

        mockMvc.perform(put("/api/users/me/profile")
                .with(SecurityTestUtils.bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Name"))
            .andExpect(jsonPath("$.bio").value("New Bio"))
            .andExpect(jsonPath("$.profileImageUrl").value("http://example.com/p.png"));
    }

    @Test
    public void blankName_updateProfile_shouldReturn400() throws Exception {
        String payload = """
                { "name": "  ", "bio": "Bio", "profileImageUrl": null }
                """;

        mockMvc.perform(put("/api/users/me/profile")
                .with(SecurityTestUtils.bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest());
    }
}

package com.mybeaufortviewproject.mybeaufortview_backend.user;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
public class UserSecurityIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User admin;
    private User privA;
    private User privB;

    private String adminToken;
    private String privAToken;

    @BeforeEach
    public void setup() {
        // Seed users directly (no controller calls)
        admin = SecurityTestUtils.seedUser(
                userRepository, passwordEncoder, "admin@example.com", Role.ADMIN);

        privA = SecurityTestUtils.seedUser(
                userRepository, passwordEncoder, "privA@example.com", Role.PRIVILEGED_USER);

        privB = SecurityTestUtils.seedUser(
                userRepository, passwordEncoder, "privB@example.com", Role.PRIVILEGED_USER);

        // Generate real JWTs for them
        adminToken = jwtUtil.generateToken(admin.getEmail(), admin.getRole().name(), admin.getId());
        privAToken = jwtUtil.generateToken(privA.getEmail(), privA.getRole().name(), privA.getId());

    }

    // ---- Authentication Tests ----

    @Test
    public void noToken_getUser_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/user/{id}", privA.getId()))
            .andExpect(status().isUnauthorized());
    }

    // --- GET /users ---

    @Test
    public void admin_getAllUsers_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/users")
                .with(SecurityTestUtils.bearer(adminToken)))
        .andExpect(status().isOk());
    }

    @Test
    public void privileged_getAllUsers_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/users")
                .with(SecurityTestUtils.bearer(privAToken)))
        .andExpect(status().isForbidden());
    }

    @Test
    public void noToken_adminEndpoint_shouldReturn401() throws Exception {
      mockMvc.perform(get("/api/users"))
          .andExpect(status().isUnauthorized());
    }

    // ---- GET /user/{id} ----
    @Test
    public void privileged_getOwnUser_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/user/{id}", privA.getId())
                .with(SecurityTestUtils.bearer(privAToken)))
        .andExpect(status().isOk());
    }

    @Test
    public void privileged_getOtherUser_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/user/{id}", privB.getId())
                .with(SecurityTestUtils.bearer(privAToken)))
        .andExpect(status().isForbidden());
    }

    // ----- PUT /user/{id} ----
    @Test
    public void privileged_updateOwnUser_shouldReturn200() throws Exception {
        String payload = """
        {
          "username": "privA_updated",
          "name": "Priv A Updated",
          "email": "privA_updated@example.com",
          "password": "NewPassword123",
          "role": "PRIVILEGED_USER"
         }
         """;

        mockMvc.perform(put("/api/user/{id}", privA.getId())
                .with(SecurityTestUtils.bearer(privAToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("privA_updated@example.com"));

    }

    @Test
    public void privileged_updateOtherUser_shouldReturn403() throws Exception {
        String payload = """
            {
            "username": "hacked!",
            "name": "Hacker",
            "email": "hacker@example.com",
            "password": "HackedPassword123",
            "role": "PRIVILEGED_USER"
            }
            """;

        mockMvc.perform(put("/api/user/{id}", privB.getId())
                .with(SecurityTestUtils.bearer(privAToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isForbidden());
        }

    @Test
    public void admin_updateAnyUser_shouldReturn200() throws Exception {
        String payload = """
            {
              "username": "privB_updated_by_admin",
              "name": "Priv B Updated by Admin",
              "email": "privB_admin_updated@example.com",
              "password": "AdminReset123",
              "role": "PRIVILEGED_USER"
            }
            """;

        mockMvc.perform(put("/api/user/{id}", privB.getId())
                .with(SecurityTestUtils.bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());
    }

    // --- DELETE /user/{id} ----
    // NOTE: This assumes that you ALLOW privileged user to delete self
    // via @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(#id, authentication)
    // and SecurityConfig allows DELETE /user/* for PRIVILEGED_USER too.

    @Test
    public void privileged_deleteOwnUser_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/user/{id}", privA.getId())
                    .with(SecurityTestUtils.bearer(privAToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    public void privileged_deleteOtherUser_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/user/{id}", privB.getId())
                    .with(SecurityTestUtils.bearer(privAToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void admin_deleteAnyUser_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/user/{id}", privB.getId())
                    .with(SecurityTestUtils.bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

}

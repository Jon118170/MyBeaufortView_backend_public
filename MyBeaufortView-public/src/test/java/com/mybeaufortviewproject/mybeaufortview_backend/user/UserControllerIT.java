package com.mybeaufortviewproject.mybeaufortview_backend.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.mybeaufortviewproject.mybeaufortview_backend.security.SecurityTestUtils;


@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional

public class UserControllerIT {

    private static final Logger Logger = LoggerFactory.getLogger(UserControllerIT.class);

    private User user;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    private Long testUserId;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @BeforeEach
    public void setup() {

        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(this.webApplicationContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();

        user =  SecurityTestUtils.seedUser(
                userRepository,
                passwordEncoder,
                "testuser@example.com",
                Role.PRIVILEGED_USER

        );

        testUserId = user.getId();
    }

    @AfterEach
    public void cleanup() {

        if (testUserId != null) {
            userRepository.deleteById(testUserId);
        }
    }

    @Test
    public void testGetUserById() throws Exception {
        Logger.info("Testing getUserById for User ID: {}", testUserId);

        String response = mockMvc.perform(get("/api/user/{id}", testUserId)
                    .with(SecurityMockMvcRequestPostProcessors.user("admin")
                            .roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(testUserId))
            .andReturn()
            .getResponse()
            .getContentAsString();

        System.out.println("getUserById response: " + response);
        Logger.info("testGetUserById completed successfully for User ID: {}", testUserId);

    }

    @Test
    public void testCreateUser() throws Exception {
        String uniqueUsername = "john_doe_" + System.currentTimeMillis();
        String uniqueEmail = "john_doe_" + System.currentTimeMillis() + "@example.com";

         // Single-line JSON for Jackson
        String newUserJson = String.format(
            "{\"username\":\"%s\",\"name\":\"John Doe\",\"email\":\"%s\",\"password\":\"SecurePass123\",\"role\":\"PRIVILEGED_USER\"}",
            uniqueUsername, uniqueEmail
        );

        Logger.info("Testing createUser with username: {} and email: {}", uniqueUsername, uniqueEmail);
        System.out.println("createUser JSON payload: " + newUserJson);

        String response = mockMvc.perform(post("/api/user")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin")
                                .roles("ADMIN"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(newUserJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value(uniqueUsername))
                    .andExpect(jsonPath("$.name").value("John Doe"))
                    .andExpect(jsonPath("$.email").value(uniqueEmail))
                    .andExpect(jsonPath("$.role").value("PRIVILEGED_USER"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            System.out.println("createUser response: " + response);
            Logger.info("testCreateUser completed successfully for username: {} and email: {}", uniqueUsername, uniqueEmail);
    }

    @Test
    public void testGetAllUsers() throws Exception {
        Logger.info("Testing getAllUsers");

        mockMvc.perform(get("/api/users")
                    .with(SecurityMockMvcRequestPostProcessors.user("admin")
                            .roles(("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        Logger.info("testGetAllUsers completed successfully");

    }

    @Test
    public void testUpdateUser() throws Exception {

        // Fetch original user to get role
        String originalRoleName = user.getRole().name();

        // Updated user JSON payload
        String updateUserJson = "{\"username\":\"johndoe_updated\",\"name\":\"John Doe Updated\",\"email\":\"johndoe_updated@example.com\",\"password\":\"newpassword1234\",\"role\":\"ADMIN\"}";

        // Perform update request
        mockMvc.perform(put("/api/user/{id}", testUserId)
                    .with(SecurityMockMvcRequestPostProcessors.user("admin")
                            .roles("ADMIN"))
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateUserJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe_updated"))
                .andExpect(jsonPath("$.name").value("John Doe Updated"))
                .andExpect(jsonPath("$.email").value("johndoe_updated@example.com"))
                .andExpect(jsonPath("$.role").value(originalRoleName));

        }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testDeleteUser() throws Exception {

        // Attempt to delete user
        mockMvc.perform(delete("/api/user/{id}", testUserId)
                .with(SecurityMockMvcRequestPostProcessors.csrf()) // Include CSRF token
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
            .andExpect(status().isNoContent()); // Expecting 204 No Content for already deleted user

        // Confirm deletion
        mockMvc.perform(get("/api/user/{id}", testUserId)
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
            .andExpect(status().isNotFound()); // Expecting 404 Not Found after deletion

       testUserId = null; // Prevent cleanup in @AfterEach
    }

}

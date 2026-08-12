package com.mybeaufortviewproject.mybeaufortview_backend.media;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.JwtUtil;
import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.storage.S3Service;
import com.mybeaufortviewproject.mybeaufortview_backend.media.dto.PresignUploadResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.security.SecurityTestUtils;
import com.mybeaufortviewproject.mybeaufortview_backend.user.Role;
import com.mybeaufortviewproject.mybeaufortview_backend.user.User;
import com.mybeaufortviewproject.mybeaufortview_backend.user.UserRepository;

import jakarta.transaction.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@Transactional
public class S3ControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private S3Service s3Service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String token;

    @BeforeEach
    public void setup() {
        User uploader = SecurityTestUtils.seedUser(
                userRepository, passwordEncoder, "uploader@example.com", Role.PRIVILEGED_USER);
        token = SecurityTestUtils.tokenFor(uploader, jwtUtil);

    }

    @Test
    public void presign_authenticated_returnsUploadUrlAndFileUrl() throws Exception {
        when(s3Service.createPresignedPutUrl("image/jpeg", "jpg"))
        .thenReturn(new PresignUploadResponse(
                "https://example.com/presigned-put",
                "https://my-bucket.s3.us-east-1.amazonaws.com/uploads/unique-file-id.jpg",
                "uploads/unique-file-id.jpg"
        ));

        String body = """
                {"contentType":"image/jpeg", "fileExtension":"jpg"}
                """;

        mockMvc.perform(post("/api/uploads/presign")
                .with(SecurityTestUtils.bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.uploadUrl").value("https://example.com/presigned-put"))
            .andExpect(jsonPath("$.fileUrl").value("https://my-bucket.s3.us-east-1.amazonaws.com/uploads/unique-file-id.jpg"))
            .andExpect(jsonPath("$.key").value("uploads/unique-file-id.jpg"));
    }

    @Test
    public void presign_unauthenticated_isRejected() throws Exception {
        String body = """
                {"contentType": "image/jpeg", "fileExtension":"jpg"}
                """;

        mockMvc.perform(post("/api/uploads/presign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());
    }

}

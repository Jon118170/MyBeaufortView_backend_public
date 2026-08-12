package com.mybeaufortviewproject.mybeaufortview_backend.commission;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybeaufortviewproject.mybeaufortview_backend.commission.dto.CommissionRequestCreate;
import com.mybeaufortviewproject.mybeaufortview_backend.commission.dto.CommissionRequestResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.UserPrincipal;

@WebMvcTest(CommissionRequestController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class CommissionRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommissionRequestService commissionRequestService;

    private final Long userId = 1L;

    @BeforeEach
    public void setUp() {
        UserPrincipal userPrincipal = new UserPrincipal(
                userId,
                "test@example.com",
                "PRIVILEGED_USER"
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userPrincipal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void createRequest_shouldReturnCreatedRequest() throws Exception {
        CommissionRequestCreate request = new CommissionRequestCreate();
        request.setPhotographerId(2L);
        request.setPostId(10L);
        request.setMessage("I'd like to commission a portrait session.");

        CommissionRequestResponse response = new CommissionRequestResponse(
                100L,
                1L,
                "requester1",
                2L,
                "photographer1",
                10L,
                CommissionRequestStatus.PENDING,
                "I'd like to commission a portrait session.",
                Instant.parse("2026-04-02T12:00:00Z"),
                Instant.parse("2026-04-02T12:00:00Z")
        );

        when(commissionRequestService.createRequest(eq(userId), any(CommissionRequestCreate.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/commissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.requesterId").value(1))
                .andExpect(jsonPath("$.requesterUsername").value("requester1"))
                .andExpect(jsonPath("$.photographerId").value(2))
                .andExpect(jsonPath("$.photographerUsername").value("photographer1"))
                .andExpect(jsonPath("$.postId").value(10))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value("I'd like to commission a portrait session."));

        verify(commissionRequestService).createRequest(eq(userId), any(CommissionRequestCreate.class));    }

    @Test
    public void getReceivedRequests_shouldReturnList() throws Exception {
        CommissionRequestResponse response = new CommissionRequestResponse(
                101L,
                3L,
                "visitor1",
                1L,
                "photographer1",
                11L,
                CommissionRequestStatus.PENDING,
                "Can you shoot family portraits?",
                Instant.parse("2026-04-02T13:00:00Z"),
                Instant.parse("2026-04-02T13:00:00Z")
        );

        when(commissionRequestService.getReceivedRequests(userId))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/commissions/received"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].requesterUsername").value("visitor1"))
                .andExpect(jsonPath("$[0].photographerUsername").value("photographer1"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(commissionRequestService).getReceivedRequests(userId);
    }

    @Test
    public void getSentRequests_shouldReturnList() throws Exception {
        CommissionRequestResponse response = new CommissionRequestResponse(
                102L,
                1L,
                "requester1",
                4L,
                "photographer2",
                null,
                CommissionRequestStatus.PENDING,
                "Would you be available for an event shoot?",
                Instant.parse("2026-04-02T14:00:00Z"),
                Instant.parse("2026-04-02T14:00:00Z")
        );

        when(commissionRequestService.getSentRequests(userId))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/commissions/sent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(102))
                .andExpect(jsonPath("$[0].requesterUsername").value("requester1"))
                .andExpect(jsonPath("$[0].photographerUsername").value("photographer2"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(commissionRequestService).getSentRequests(userId);
    }

    @Test
    public void acceptRequest_shouldReturnUpdatedRequest() throws Exception {
        Long requestId = 200L;

        CommissionRequestResponse response = new CommissionRequestResponse(
                requestId,
                3L,
                "visitor1",
                1L,
                "photographer1",
                10L,
                CommissionRequestStatus.ACCEPTED,
                "Can you shoot a Beaufort waterfront session?",
                Instant.parse("2026-04-02T15:00:00Z"),
                Instant.parse("2026-04-02T15:30:00Z")
        );

        when(commissionRequestService.acceptRequest(requestId, userId)).thenReturn(response);

        mockMvc.perform(patch("/api/commissions/{id}/accept", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(200))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        verify(commissionRequestService).acceptRequest(requestId, userId);
    }

    @Test
    public void declineRequest_shouldReturnUpdatedRequest() throws Exception {
        Long requestId = 201L;

        CommissionRequestResponse response = new CommissionRequestResponse(
                requestId,
                3L,
                "visitor1",
                1L,
                "photographer1",
                10L,
                CommissionRequestStatus.DECLINED,
                "Can you shoot a Beaufort waterfront session?",
                Instant.parse("2026-04-02T16:00:00Z"),
                Instant.parse("2026-04-02T16:30:00Z")
        );

        when(commissionRequestService.declineRequest(requestId, userId)).thenReturn(response);

        mockMvc.perform(patch("/api/commissions/{id}/decline", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(201))
                .andExpect(jsonPath("$.status").value("DECLINED"));

        verify(commissionRequestService).declineRequest(requestId, userId);
    }
}

package com.mybeaufortviewproject.mybeaufortview_backend.media;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mybeaufortviewproject.mybeaufortview_backend.media.dto.MediaJobStatusResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.media.dto.MediaStatusResponse;

@WebMvcTest(MediaController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class MediaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaJobService mediaJobService;

    @Test
    public void getMediaStatus_shouldReturnMediaStatusResponse() throws Exception {
        MediaJobStatusResponse jobDto = new MediaJobStatusResponse(
                1L,
                "AI_TAGGING",
                "COMPLETED",
                1,
                null
        );

        MediaStatusResponse response = new MediaStatusResponse(
                42L,
                List.of(jobDto)
        );

        when(mediaJobService.getMediaStatusForPost(42L)).thenReturn(response);

        mockMvc.perform(get("/api/media/42/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(42))
                .andExpect(jsonPath("$.jobs[0].id").value(1))
                .andExpect(jsonPath("$.jobs[0].jobType").value("AI_TAGGING"))
                .andExpect(jsonPath("$.jobs[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.jobs[0].attemptCount").value(1));
    }

    @Test
    public void getMediaStatus_shouldReturnEmptyJobsList() throws Exception {
        MediaStatusResponse response = new MediaStatusResponse(
                99L,
                List.of()
        );

        when(mediaJobService.getMediaStatusForPost(99L)).thenReturn(response);

        mockMvc.perform(get("/api/media/99/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(99))
                .andExpect(jsonPath("$.jobs").isArray())
                .andExpect(jsonPath("$.jobs").isEmpty());
    }
}

package com.mybeaufortviewproject.mybeaufortview_backend.collection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mybeaufortviewproject.mybeaufortview_backend.collection.CollectionVisibility;
import com.mybeaufortviewproject.mybeaufortview_backend.collection.dto.CollectionResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.UserPrincipal;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.AuthorResponse;

@ActiveProfiles("test")
@WebMvcTest(controllers = UserCollectionController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserCollectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CollectionService collectionService;

    // Security principal wiring verified in security-focused tests

    @Test
    public void getCollectionsByUser_returnsPage() throws Exception {

        // Mock data
        AuthorResponse owner = new AuthorResponse(10L, "photog1", "Jane Doe", null);

        CollectionResponse response =
                new CollectionResponse(
                        1L,
                        "Lowcountry Stories",
                        owner,
                        5L,
                        "https://example.com/cover.jpg",
                        CollectionVisibility.PUBLIC);

        // Mock page
        Page<CollectionResponse> page =
                new PageImpl<>(List.of(response), PageRequest.of(0, 12), 1);

        // Mock service
        when(collectionService.getCollectionsByUserId(
                eq(10L),
                any(Pageable.class),
                nullable(UserPrincipal.class)
            )).thenReturn(page);

        // Perform request and assert
        mockMvc.perform(get("/api/users/10/collections?page=0&size=12"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value(1))
            .andExpect(jsonPath("$.items[0].owner.id").value(10))
            .andExpect(jsonPath("$.items[0].title").value("Lowcountry Stories"))
            .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    public void getCollectionsByUser_anonymous_principalNull() throws Exception {
        AuthorResponse owner =
                new AuthorResponse(10L, "photog1", "Jane Doe", null);

        CollectionResponse response =
                new CollectionResponse(1L, "Lowcountry Stories", owner, 5L, "https://example.com/cover.jpg", CollectionVisibility.PUBLIC);

        Page<CollectionResponse> page =
                new PageImpl<>(List.of(response), PageRequest.of(0, 12), 1);

        // Principal is null for anonymous user
        when(collectionService.getCollectionsByUserId(
                eq(10L),
                any(Pageable.class),
                isNull()
        )).thenReturn(page);

        mockMvc.perform(get("/api/users/10/collections?page=0&size=12").with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(1))
        .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    public void getCollectionsByUser_authenticated_principalProvided() throws Exception {
        AuthorResponse owner = new AuthorResponse(10L, "photog1", "Jane Doe", null);
        CollectionResponse response =
                new CollectionResponse(2L, "Private Set", owner, 1L, "https://example.com/private.jpg", CollectionVisibility.PRIVATE);

        Page<CollectionResponse> page =
                new PageImpl<>(List.of(response), PageRequest.of(0, 12), 1);

        UserPrincipal principal = new UserPrincipal(10L, "jane@example.com", "USER");
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        when(collectionService.getCollectionsByUserId(
                eq(10L),
                any(Pageable.class),
                any()
        )).thenReturn(page);

        mockMvc.perform(get("/api/users/10/collections?page=0&size=12")
                .with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value(2))
            .andExpect(jsonPath("$.totalItems").value(1));
    }
}

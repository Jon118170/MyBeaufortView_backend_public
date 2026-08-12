package com.mybeaufortviewproject.mybeaufortview_backend.commission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.ErrorCode;
import com.mybeaufortviewproject.mybeaufortview_backend.commission.dto.CommissionRequestCreate;
import com.mybeaufortviewproject.mybeaufortview_backend.commission.dto.CommissionRequestResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.ConflictException;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.NotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.PostNotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.UserNotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.notification.NotificationService;
import com.mybeaufortviewproject.mybeaufortview_backend.notification.NotificationType;
import com.mybeaufortviewproject.mybeaufortview_backend.post.Post;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.user.Role;
import com.mybeaufortviewproject.mybeaufortview_backend.user.User;
import com.mybeaufortviewproject.mybeaufortview_backend.user.UserRepository;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class CommissionRequestServiceTest {

    @Mock
    private CommissionRequestRepository commissionRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommissionRequestService commissionRequestService;

    @Captor
    private ArgumentCaptor<CommissionRequest> commissionRequestCaptor;

    private User requester;
    private User photographer;
    private Post post;

    @BeforeEach
    public void setUp() {
        requester = new User();
        requester.setId(1L);
        requester.setUsername("requester1");
        requester.setName("Requester One");
        requester.setEmail("requester@example.com");
        requester.setPassword("password");
        requester.setRole(Role.PRIVILEGED_USER);

        photographer = new User();
        photographer.setId(2L);
        photographer.setUsername("photographer1");
        photographer.setName("Photographer One");
        photographer.setEmail("photographer@example.com");
        photographer.setPassword("password");
        photographer.setRole(Role.PRIVILEGED_USER);

        post = new Post();
        post.setId(10L);
        post.setUser(photographer);
    }

    @Test
    public void createRequest_shouldCreatePendingCommissionRequest() {
        CommissionRequestCreate request = new CommissionRequestCreate();
        request.setPhotographerId(2L);
        request.setPostId(10L);
        request.setMessage("I'd love to commission you for a Beaufort waterfront shoot.");

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(userRepository.findById(2L)).thenReturn(Optional.of(photographer));
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(commissionRequestRepository.save(org.mockito.ArgumentMatchers.any(CommissionRequest.class)))
                .thenAnswer(invocation -> {
                    CommissionRequest saved = invocation.getArgument(0);
                    saved.setId(100L);
                    saved.setCreatedAt(Instant.parse("2026-04-02T12:00:00Z"));
                    saved.setUpdatedAt(Instant.parse("2026-04-02T12:00:00Z"));
                    return saved;
                });

        CommissionRequestResponse response = commissionRequestService.createRequest(1L, request);

        verify(commissionRequestRepository).save(commissionRequestCaptor.capture());
        CommissionRequest saved = commissionRequestCaptor.getValue();

        assertEquals(requester, saved.getRequester());
        assertEquals(photographer, saved.getPhotographer());
        assertEquals(post, saved.getPost());
        assertEquals("I'd love to commission you for a Beaufort waterfront shoot.", saved.getMessage());
        assertEquals(CommissionRequestStatus.PENDING, saved.getStatus());

        assertEquals(100L, response.id());
        assertEquals(1L, response.requesterId());
        assertEquals("requester1", response.requesterUsername());
        assertEquals(2L, response.photographerId());
        assertEquals("photographer1", response.photographerUsername());
        assertEquals(10L, response.postId());
        assertEquals(CommissionRequestStatus.PENDING, response.status());
        assertEquals("I'd love to commission you for a Beaufort waterfront shoot.", response.message());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());

        verify(notificationService).createNotification(
                eq(photographer),
                eq(NotificationType.COMMISSION_REQUEST_RECEIVED),
                eq("New commission request"),
                contains("sent you a commission request."),
                eq(10L),
                eq(100L)
         );
    }

    @Test
    public void createRequest_shouldThrowWhenRequesterTargetsSelf() {
        CommissionRequestCreate request = new CommissionRequestCreate();
        request.setPhotographerId(1L);
        request.setMessage("This should fail.");

        assertThrows(ConflictException.class,
                () -> commissionRequestService.createRequest(1L, request));

        verify(userRepository, never()).findById(org.mockito.ArgumentMatchers.anyLong());
        verify(commissionRequestRepository, never()).save(org.mockito.ArgumentMatchers.any());

        verify(notificationService, never()).createNotification(
                any(), any(), anyString(), anyString(), any(), any()
            );
    }

    @Test
    public void createRequest_shouldThrowWhenPhotographerNotFound() {
        CommissionRequestCreate request = new CommissionRequestCreate();
        request.setPhotographerId(2L);
        request.setMessage("Please photograph our event.");

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> commissionRequestService.createRequest(1L, request));

        verify(notificationService, never()).createNotification(
                any(), any(), anyString(), anyString(), any(), any()
            );
    }

    @Test
    public void createRequest_shouldThrowWhenPostNotFound() {
        CommissionRequestCreate request = new CommissionRequestCreate();
        request.setPhotographerId(2L);
        request.setPostId(10L);
        request.setMessage("Please photograph our event.");

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(userRepository.findById(2L)).thenReturn(Optional.of(photographer));
        when(postRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class,
                () -> commissionRequestService.createRequest(1L, request));

        verify(notificationService, never()).createNotification(
                any(), any(), anyString(), anyString(), any(), any()
            );
    }

    @Test
    public void getReceivedRequests_shouldReturnMappedRequests() {
        CommissionRequest commissionRequest = new CommissionRequest();
        commissionRequest.setId(200L);
        commissionRequest.setRequester(requester);
        commissionRequest.setPhotographer(photographer);
        commissionRequest.setPost(post);
        commissionRequest.setMessage("Can you shoot portraits downtown?");
        commissionRequest.setStatus(CommissionRequestStatus.PENDING);
        commissionRequest.setCreatedAt(Instant.parse("2026-04-02T13:00:00Z"));
        commissionRequest.setUpdatedAt(Instant.parse("2026-04-02T13:00:00Z"));

        when(commissionRequestRepository.findByPhotographerIdOrderByCreatedAtDesc(2L))
                .thenReturn(List.of(commissionRequest));

        List<CommissionRequestResponse> result = commissionRequestService.getReceivedRequests(2L);

        assertEquals(1, result.size());
        assertEquals(200L, result.get(0).id());
        assertEquals("requester1", result.get(0).requesterUsername());
        assertEquals("photographer1", result.get(0).photographerUsername());
        assertEquals(10L, result.get(0).postId());
    }

    @Test
    public void getSentRequests_shouldReturnMappedRequests() {
        CommissionRequest commissionRequest = new CommissionRequest();
        commissionRequest.setId(201L);
        commissionRequest.setRequester(requester);
        commissionRequest.setPhotographer(photographer);
        commissionRequest.setPost(post);
        commissionRequest.setMessage("Can you shoot portraits downtown?");
        commissionRequest.setStatus(CommissionRequestStatus.PENDING);
        commissionRequest.setCreatedAt(Instant.parse("2026-04-02T14:00:00Z"));
        commissionRequest.setUpdatedAt(Instant.parse("2026-04-02T14:00:00Z"));

        when(commissionRequestRepository.findByRequesterIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(commissionRequest));

        List<CommissionRequestResponse> result = commissionRequestService.getSentRequests(1L);

        assertEquals(1, result.size());
        assertEquals(201L, result.get(0).id());
        assertEquals("requester1", result.get(0).requesterUsername());
        assertEquals("photographer1", result.get(0).photographerUsername());
        assertEquals(CommissionRequestStatus.PENDING, result.get(0).status());
    }

    @Test
    public void acceptRequest_shouldSetStatusAccepted() {
        CommissionRequest commissionRequest = new CommissionRequest();
        commissionRequest.setId(300L);
        commissionRequest.setRequester(requester);
        commissionRequest.setPhotographer(photographer);
        commissionRequest.setPost(post);
        commissionRequest.setMessage("Please shoot a marsh sunset session.");
        commissionRequest.setStatus(CommissionRequestStatus.PENDING);
        commissionRequest.setCreatedAt(Instant.parse("2026-04-02T15:00:00Z"));
        commissionRequest.setUpdatedAt(Instant.parse("2026-04-02T15:00:00Z"));

        when(commissionRequestRepository.findByIdAndPhotographerId(300L, 2L))
                .thenReturn(Optional.of(commissionRequest));

        CommissionRequestResponse response = commissionRequestService.acceptRequest(300L, 2L);

        assertEquals(CommissionRequestStatus.ACCEPTED, commissionRequest.getStatus());
        assertEquals(CommissionRequestStatus.ACCEPTED, response.status());

        verify(notificationService).createNotification(
                eq(requester),
                eq(NotificationType.COMMISSION_REQUEST_ACCEPTED),
                eq("Commission request accepted"),
                contains("accepted your commission request"),
                eq(10L),
                eq(300L)
        );
    }

    @Test
    public void declineRequest_shouldSetStatusDeclined() {
        CommissionRequest commissionRequest = new CommissionRequest();
        commissionRequest.setId(301L);
        commissionRequest.setRequester(requester);
        commissionRequest.setPhotographer(photographer);
        commissionRequest.setPost(post);
        commissionRequest.setMessage("Please shoot a marsh sunset session.");
        commissionRequest.setStatus(CommissionRequestStatus.PENDING);
        commissionRequest.setCreatedAt(Instant.parse("2026-04-02T16:00:00Z"));
        commissionRequest.setUpdatedAt(Instant.parse("2026-04-02T16:00:00Z"));

        when(commissionRequestRepository.findByIdAndPhotographerId(301L, 2L))
                .thenReturn(Optional.of(commissionRequest));

        CommissionRequestResponse response = commissionRequestService.declineRequest(301L, 2L);

        assertEquals(CommissionRequestStatus.DECLINED, commissionRequest.getStatus());
        assertEquals(CommissionRequestStatus.DECLINED, response.status());

        verify(notificationService).createNotification(
                eq(requester),
                eq(NotificationType.COMMISSION_REQUEST_DECLINED),
                eq("Commission request declined"),
                contains("declined your commission request"),
                eq(10L),
                eq(301L)
        );

    }

    @Test
    public void acceptRequest_shouldThrowWhenRequestNotPending() {
        CommissionRequest commissionRequest = new CommissionRequest();
        commissionRequest.setId(400L);
        commissionRequest.setPhotographer(photographer);
        commissionRequest.setStatus(CommissionRequestStatus.ACCEPTED);

        when(commissionRequestRepository.findByIdAndPhotographerId(400L, 2L))
                .thenReturn(Optional.of(commissionRequest));

        assertThrows(ConflictException.class,
                () -> commissionRequestService.acceptRequest(400L, 2L));

        verify(notificationService, never()).createNotification(
                any(), any(), anyString(), anyString(), any(), any()
            );
    }

    @Test
    public void declineRequest_shouldThrowWhenRequestNotPending() {
        CommissionRequest commissionRequest = new CommissionRequest();
        commissionRequest.setId(401L);
        commissionRequest.setPhotographer(photographer);
        commissionRequest.setStatus(CommissionRequestStatus.DECLINED);

        when(commissionRequestRepository.findByIdAndPhotographerId(401L, 2L))
                .thenReturn(Optional.of(commissionRequest));

        assertThrows(ConflictException.class,
                () -> commissionRequestService.declineRequest(401L, 2L));

        verify(notificationService, never()).createNotification(
                any(), any(), anyString(), anyString(), any(), any()
            );
    }

    @Test
    public void acceptRequest_shouldThrowWhenRequestNotFound() {
        when(commissionRequestRepository.findByIdAndPhotographerId(500L, 2L))
                .thenReturn(Optional.empty());

        NotFoundException ex =
        assertThrows(NotFoundException.class,
                () -> commissionRequestService.acceptRequest(500L, 2L));

        assertEquals(
                ErrorCode.COMMISSION_REQUEST_NOT_FOUND,
                ex.getCode()
        );

        verify(notificationService, never()).createNotification(
                any(), any(), anyString(), anyString(), any(), any()
            );
    }

    @Test
    public void declineRequest_shouldThrowWhenRequestNotFound() {
        when(commissionRequestRepository.findByIdAndPhotographerId(501L, 2L))
                .thenReturn(Optional.empty());

        NotFoundException ex =
        assertThrows(NotFoundException.class,
                () -> commissionRequestService.declineRequest(501L, 2L));

        assertEquals(
                ErrorCode.COMMISSION_REQUEST_NOT_FOUND,
                ex.getCode()
        );

        verify(notificationService, never()).createNotification(
                any(), any(), anyString(), anyString(), any(), any()
            );
    }
}

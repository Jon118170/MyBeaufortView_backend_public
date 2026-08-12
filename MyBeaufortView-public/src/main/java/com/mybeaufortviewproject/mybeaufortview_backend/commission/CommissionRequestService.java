package com.mybeaufortviewproject.mybeaufortview_backend.commission;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mybeaufortviewproject.mybeaufortview_backend.commission.dto.CommissionRequestCreate;
import com.mybeaufortviewproject.mybeaufortview_backend.commission.dto.CommissionRequestResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.ConflictException;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.ErrorCode;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.NotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.PostNotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.UserNotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.notification.NotificationService;
import com.mybeaufortviewproject.mybeaufortview_backend.notification.NotificationType;
import com.mybeaufortviewproject.mybeaufortview_backend.post.Post;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostRepository;
import com.mybeaufortviewproject.mybeaufortview_backend.user.User;
import com.mybeaufortviewproject.mybeaufortview_backend.user.UserRepository;

@Service
@Transactional
public class CommissionRequestService {

    private static final String NEW_REQUEST_TITLE = "New commission request";
    private static final String ACCEPTED_TITLE = "Commission request accepted";
    private static final String DECLINED_TITLE = "Commission request declined";

    private final CommissionRequestRepository commissionRequestRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final NotificationService notificationService;

    public CommissionRequestService(
            CommissionRequestRepository commissionRequestRepository,
            UserRepository userRepository,
            PostRepository postRepository,
            NotificationService notificationService
    ) {
        this.commissionRequestRepository = commissionRequestRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.notificationService = notificationService;
    }

    // Create a new commission request - requester cannot be the same as photographer, and post must exist if provided
    public CommissionRequestResponse createRequest(Long requesterId, CommissionRequestCreate request) {
        if (requesterId.equals(request.getPhotographerId())) {
            throw new ConflictException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "You cannot request a commission from yourself.");
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new UserNotFoundException(requesterId));

        User photographer = userRepository.findById(request.getPhotographerId())
                .orElseThrow(() -> new UserNotFoundException(request.getPhotographerId()));

        Post post = null;
        if (request.getPostId() != null) {
            post = postRepository.findById(request.getPostId())
                    .orElseThrow(() -> new PostNotFoundException(request.getPostId()));
        }

        CommissionRequest commissionRequest = new CommissionRequest();
        commissionRequest.setRequester(requester);
        commissionRequest.setPhotographer(photographer);
        commissionRequest.setPost(post);
        commissionRequest.setMessage(request.getMessage());
        commissionRequest.setStatus(CommissionRequestStatus.PENDING);

        CommissionRequest saved = commissionRequestRepository.save(commissionRequest);

        notificationService.createNotification(
                photographer,
                NotificationType.COMMISSION_REQUEST_RECEIVED,
                NEW_REQUEST_TITLE,
                buildRequestMessage(requester),
                getPostId(saved),
                saved.getId()
        );

        return toCommissionRequestResponse(saved);
    }

    // Get received requests for a photographer, ordered by most recent first
    @Transactional(readOnly = true)
    public List<CommissionRequestResponse> getReceivedRequests(Long photographerId) {
        return commissionRequestRepository.findByPhotographerIdOrderByCreatedAtDesc(photographerId)
                .stream()
                .map(this::toCommissionRequestResponse)
                .toList();
    }

    // Get sent requests for a requester, ordered by most recent first
    @Transactional(readOnly = true)
    public List<CommissionRequestResponse> getSentRequests(Long requesterId) {
        return commissionRequestRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId)
                .stream()
                .map(this::toCommissionRequestResponse)
                .toList();
    }

    // Accept a commission request - only the photographer can accept, and only if it's pending
    public CommissionRequestResponse acceptRequest(Long requestId, Long photographerId) {
        CommissionRequest commissionRequest = commissionRequestRepository.findByIdAndPhotographerId(requestId, photographerId)
                .orElseThrow(() ->
                    new NotFoundException(
                            ErrorCode.COMMISSION_REQUEST_NOT_FOUND,
                            "Commission request not found with id " + requestId + " for photographer " + photographerId
                )
        );

        if (commissionRequest.getStatus() != CommissionRequestStatus.PENDING) {
            throw new ConflictException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "Only pending commission requests can be accepted."
            );
        }

        commissionRequest.setStatus(CommissionRequestStatus.ACCEPTED);

        notificationService.createNotification(
                commissionRequest.getRequester(),
                NotificationType.COMMISSION_REQUEST_ACCEPTED,
                ACCEPTED_TITLE,
                buildAcceptedMessage(commissionRequest.getPhotographer()),
                getPostId(commissionRequest),
                commissionRequest.getId()
            );

        return toCommissionRequestResponse(commissionRequest);
    }

    // Decline a commission request - only the photographer can decline, and only if it's pending
    public CommissionRequestResponse declineRequest(Long requestId, Long photographerId) {
        CommissionRequest commissionRequest = commissionRequestRepository.findByIdAndPhotographerId(requestId, photographerId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.COMMISSION_REQUEST_NOT_FOUND,
                        "Commission request not found with id " + requestId + " for photographer " + photographerId
                )
          );

        if (commissionRequest.getStatus() != CommissionRequestStatus.PENDING) {
            throw new ConflictException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "Only pending commission requests can be declined."
           );
        }

        commissionRequest.setStatus(CommissionRequestStatus.DECLINED);

        notificationService.createNotification(
                commissionRequest.getRequester(),
                NotificationType.COMMISSION_REQUEST_DECLINED,
                DECLINED_TITLE,
                buildDeclinedMessage(commissionRequest.getPhotographer()),
                getPostId(commissionRequest),
                commissionRequest.getId()
            );

        return toCommissionRequestResponse(commissionRequest);
    }

    // Helper method to convert CommissionRequest entity to CommissionRequestResponse DTO
    private CommissionRequestResponse toCommissionRequestResponse(CommissionRequest commissionRequest) {
        return new CommissionRequestResponse(
                commissionRequest.getId(),
                commissionRequest.getRequester().getId(),
                commissionRequest.getRequester().getUsername(),
                commissionRequest.getPhotographer().getId(),
                commissionRequest.getPhotographer().getUsername(),
                commissionRequest.getPost() != null ? commissionRequest.getPost().getId() : null,
                commissionRequest.getStatus(),
                commissionRequest.getMessage(),
                commissionRequest.getCreatedAt(),
                commissionRequest.getUpdatedAt()
        );
    }

    // Helper methods to build notification messages
    private String buildRequestMessage(User requester) {
        return requester.getUsername() + " sent you a commission request.";
    }

    // Build notification message for accepted request
    private String buildAcceptedMessage(User photographer) {
        return photographer.getUsername() + " accepted your commission request.";
    }

    // Build notification message for declined request
    private String buildDeclinedMessage(User photographer) {
        return photographer.getUsername() + " declined your commission request.";
    }

    // Helper method to safely get post ID for notifications (returns null if no post associated)
    private Long getPostId(CommissionRequest request) {
        return request.getPost() != null ? request.getPost().getId() : null;
    }


}

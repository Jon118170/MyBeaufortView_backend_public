package com.mybeaufortviewproject.mybeaufortview_backend.commission.dto;

import java.time.Instant;

import com.mybeaufortviewproject.mybeaufortview_backend.commission.CommissionRequestStatus;

public record CommissionRequestResponse(
        Long id,
        Long requesterId,
        String requesterUsername,
        Long photographerId,
        String photographerUsername,
        Long postId,
        CommissionRequestStatus status,
        String message,
        Instant createdAt,
        Instant updatedAt
) {
}

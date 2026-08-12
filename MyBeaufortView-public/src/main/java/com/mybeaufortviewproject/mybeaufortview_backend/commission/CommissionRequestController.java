package com.mybeaufortviewproject.mybeaufortview_backend.commission;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mybeaufortviewproject.mybeaufortview_backend.commission.dto.CommissionRequestCreate;
import com.mybeaufortviewproject.mybeaufortview_backend.commission.dto.CommissionRequestResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security.UserPrincipal;

@RestController
@RequestMapping("/api/commissions")
public class CommissionRequestController {

    private final CommissionRequestService commissionRequestService;

    public CommissionRequestController(CommissionRequestService commissionRequestService) {
        this.commissionRequestService = commissionRequestService;
    }

    @PostMapping
    public CommissionRequestResponse createRequest(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody CommissionRequestCreate request) {

        return commissionRequestService.createRequest(userPrincipal.id(), request);
    }

    @GetMapping("/received")
    public List<CommissionRequestResponse> getReceivedRequests(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        return commissionRequestService.getReceivedRequests(userPrincipal.id());
    }

    @GetMapping("/sent")
    public List<CommissionRequestResponse> getSentRequests(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        return commissionRequestService.getSentRequests(userPrincipal.id());
    }

    @PatchMapping("/{id}/accept")
    public CommissionRequestResponse acceptRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        return commissionRequestService.acceptRequest(id, userPrincipal.id());
    }

    @PatchMapping("/{id}/decline")
    public CommissionRequestResponse declineRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        return commissionRequestService.declineRequest(id, userPrincipal.id());
    }
}

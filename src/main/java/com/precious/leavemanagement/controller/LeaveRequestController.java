package com.precious.leavemanagement.controller;

import com.precious.leavemanagement.dto.request.LeaveRequestDto;
import com.precious.leavemanagement.dto.request.LeaveReviewRequest;
import com.precious.leavemanagement.dto.response.LeaveRequestResponse;
import com.precious.leavemanagement.service.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave-requests")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Leave Requests", description = "Leave request management endpoints")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @PostMapping
    @Operation(summary = "Submit a new leave request")
    public ResponseEntity<LeaveRequestResponse> submitLeaveRequest(@Valid @RequestBody LeaveRequestDto request) {
        LeaveRequestResponse response = leaveRequestService.submitLeaveRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-requests")
    @Operation(summary = "Get my leave requests")
    public ResponseEntity<List<LeaveRequestResponse>> getMyLeaveRequests() {
        List<LeaveRequestResponse> requests = leaveRequestService.getMyLeaveRequests();
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Get all pending leave requests (Manager only)")
    public ResponseEntity<List<LeaveRequestResponse>> getPendingLeaveRequests() {
        List<LeaveRequestResponse> requests = leaveRequestService.getPendingLeaveRequests();
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get leave request by ID")
    public ResponseEntity<LeaveRequestResponse> getLeaveRequestById(@PathVariable Long id) {
        LeaveRequestResponse response = leaveRequestService.getLeaveRequestById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Approve a leave request (Manager only)")
    public ResponseEntity<LeaveRequestResponse> approveLeave(@PathVariable Long id) {
        LeaveRequestResponse response = leaveRequestService.approveLeave(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Reject a leave request (Manager only)")
    public ResponseEntity<LeaveRequestResponse> rejectLeave(
            @PathVariable Long id,
            @Valid @RequestBody LeaveReviewRequest reviewRequest) {
        LeaveRequestResponse response = leaveRequestService.rejectLeave(id, reviewRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a leave request")
    public ResponseEntity<Void> cancelLeaveRequest(@PathVariable Long id) {
        leaveRequestService.cancelLeaveRequest(id);
        return ResponseEntity.noContent().build();
    }
}

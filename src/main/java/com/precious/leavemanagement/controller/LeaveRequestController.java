package com.precious.leavemanagement.controller;

import com.precious.leavemanagement.dto.request.LeaveRequestDto;
import com.precious.leavemanagement.dto.request.LeaveReviewRequest;
import com.precious.leavemanagement.dto.response.LeaveRequestResponse;
import com.precious.leavemanagement.service.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Leave Requests", description = "Leave request management endpoints. Employees can submit, view, and cancel their own requests. Managers can approve/reject requests.")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @PostMapping("/submit")
    @Operation(
            summary = "Submit a new leave request",
            description = "Employee submits a leave request. System validates: date range, available balance, no overlapping requests. Returns 201 with created request."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Leave request submitted successfully",
                    content = @Content(schema = @Schema(implementation = LeaveRequestResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request (end date before start date, insufficient balance)",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"message\": \"Insufficient leave balance\"}"))),
            @ApiResponse(responseCode = "409", description = "Overlapping leave request exists",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"message\": \"Overlapping leave request found\"}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Leave request details with leave type, date range, and reason",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = LeaveRequestDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "leaveTypeId": 1,
                              "startDate": "2026-06-01",
                              "endDate": "2026-06-05",
                              "reason": "Family vacation"
                            }
                            """)
            )
    )
    public ResponseEntity<LeaveRequestResponse> submitLeaveRequest(@Valid @RequestBody LeaveRequestDto request) {
        LeaveRequestResponse response = leaveRequestService.submitLeaveRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-requests")
    @Operation(
            summary = "Get my leave requests",
            description = "Retrieve all leave requests for the authenticated employee (all statuses: PENDING, APPROVED, REJECTED, CANCELLED)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved leave requests",
                    content = @Content(schema = @Schema(implementation = LeaveRequestResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token")
    })
    public ResponseEntity<List<LeaveRequestResponse>> getMyLeaveRequests() {
        List<LeaveRequestResponse> requests = leaveRequestService.getMyLeaveRequests();
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
            summary = "Get all pending leave requests (Manager only)",
            description = "Retrieve all leave requests with PENDING status across all employees. Only accessible to users with MANAGER role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved pending requests",
                    content = @Content(schema = @Schema(implementation = LeaveRequestResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have MANAGER role")
    })
    public ResponseEntity<List<LeaveRequestResponse>> getPendingLeaveRequests() {
        List<LeaveRequestResponse> requests = leaveRequestService.getPendingLeaveRequests();
        return ResponseEntity.ok(requests);
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
            summary = "Approve a leave request (Manager only)",
            description = "Manager approves a PENDING leave request. System deducts leave days from employee's balance. Manager cannot approve their own leave."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Leave request approved successfully",
                    content = @Content(schema = @Schema(implementation = LeaveRequestResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request (not in PENDING status, insufficient balance)",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"message\": \"Leave request is not in PENDING status\"}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have MANAGER role or attempting to approve own leave"),
            @ApiResponse(responseCode = "404", description = "Leave request not found")
    })
    public ResponseEntity<LeaveRequestResponse> approveLeave(
            @Parameter(description = "Leave request ID", example = "1") @PathVariable Long id) {
        LeaveRequestResponse response = leaveRequestService.approveLeave(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
            summary = "Reject a leave request (Manager only)",
            description = "Manager rejects a PENDING leave request with a reason. No balance deduction occurs."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Leave request rejected successfully",
                    content = @Content(schema = @Schema(implementation = LeaveRequestResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request (not in PENDING status, missing rejection reason)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have MANAGER role"),
            @ApiResponse(responseCode = "404", description = "Leave request not found")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Rejection details with mandatory reason",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = LeaveReviewRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "rejectionReason": "Insufficient staffing during requested period"
                            }
                            """)
            )
    )
    public ResponseEntity<LeaveRequestResponse> rejectLeave(
            @Parameter(description = "Leave request ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody LeaveReviewRequest reviewRequest) {
        LeaveRequestResponse response = leaveRequestService.rejectLeave(id, reviewRequest);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/cancel")
    @Operation(
            summary = "Cancel a leave request (Employee - own requests only)",
            description = "Employee cancels their own leave request. If request was APPROVED, leave days are restored to balance. Only request owner can cancel."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Leave request cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request (already cancelled, cannot cancel rejected request)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User is not the request owner"),
            @ApiResponse(responseCode = "404", description = "Leave request not found")
    })
    public ResponseEntity<Void> cancelLeaveRequest(
            @Parameter(description = "Leave request ID", example = "1") @PathVariable Long id) {
        leaveRequestService.cancelLeaveRequest(id);
        return ResponseEntity.ok().build();
    }
}

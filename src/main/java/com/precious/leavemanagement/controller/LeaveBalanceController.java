package com.precious.leavemanagement.controller;

import com.precious.leavemanagement.dto.response.LeaveBalanceResponse;
import com.precious.leavemanagement.service.LeaveBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Leave Balances", description = "Leave balance management endpoints. Employees can view their own balance. Managers can view any employee's balance.")
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    public LeaveBalanceController(LeaveBalanceService leaveBalanceService) {
        this.leaveBalanceService = leaveBalanceService;
    }

    @GetMapping("/balance")
    @Operation(
            summary = "Get my leave balances",
            description = "Retrieve authenticated employee's leave balances for all leave types (Annual, Sick, Family Responsibility). Shows available days, used days, and remaining days for current year."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved leave balances",
                    content = @Content(schema = @Schema(implementation = LeaveBalanceResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<List<LeaveBalanceResponse>> getMyLeaveBalances() {
        List<LeaveBalanceResponse> balances = leaveBalanceService.getMyLeaveBalances();
        return ResponseEntity.ok(balances);
    }

    @GetMapping("/balance/{userId}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
            summary = "Get leave balances for a specific user (Manager only)",
            description = "Retrieve leave balances for any employee by user ID. Only accessible to users with MANAGER role. Returns all leave types with current year balances."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user's leave balances",
                    content = @Content(schema = @Schema(implementation = LeaveBalanceResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have MANAGER role"),
            @ApiResponse(responseCode = "404", description = "User not found with id: {userId}")
    })
    public ResponseEntity<List<LeaveBalanceResponse>> getUserLeaveBalances(
            @Parameter(description = "User ID", example = "1") @PathVariable Long userId) {
        List<LeaveBalanceResponse> balances = leaveBalanceService.getUserLeaveBalances(userId);
        return ResponseEntity.ok(balances);
    }
}

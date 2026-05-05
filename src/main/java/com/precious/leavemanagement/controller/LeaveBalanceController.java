package com.precious.leavemanagement.controller;

import com.precious.leavemanagement.dto.response.LeaveBalanceResponse;
import com.precious.leavemanagement.service.LeaveBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave-balances")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Leave Balances", description = "Leave balance management endpoints")
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    public LeaveBalanceController(LeaveBalanceService leaveBalanceService) {
        this.leaveBalanceService = leaveBalanceService;
    }

    @GetMapping("/my-balances")
    @Operation(summary = "Get my leave balances")
    public ResponseEntity<List<LeaveBalanceResponse>> getMyLeaveBalances() {
        List<LeaveBalanceResponse> balances = leaveBalanceService.getMyLeaveBalances();
        return ResponseEntity.ok(balances);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Get leave balances for a specific user (Manager only)")
    public ResponseEntity<List<LeaveBalanceResponse>> getUserLeaveBalances(@PathVariable Long userId) {
        List<LeaveBalanceResponse> balances = leaveBalanceService.getUserLeaveBalances(userId);
        return ResponseEntity.ok(balances);
    }
}

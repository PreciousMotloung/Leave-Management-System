package com.precious.leavemanagement.controller;

import com.precious.leavemanagement.dto.response.LeaveTypeResponse;
import com.precious.leavemanagement.service.LeaveTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Leave Types", description = "Leave type information endpoints. All authenticated users can view available leave types.")
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    public LeaveTypeController(LeaveTypeService leaveTypeService) {
        this.leaveTypeService = leaveTypeService;
    }

    @GetMapping("/types")
    @Operation(
            summary = "Get all leave types",
            description = "Retrieve all available leave types in the system (Annual Leave, Sick Leave, Family Responsibility). Shows default days allocated per type. Accessible to all authenticated users (EMPLOYEE and MANAGER)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved leave types",
                    content = @Content(
                            schema = @Schema(implementation = LeaveTypeResponse.class),
                            examples = @ExampleObject(value = """
                                    [
                                      {
                                        "id": 1,
                                        "name": "ANNUAL_LEAVE",
                                        "defaultDays": 21,
                                        "description": "Annual leave for vacation and personal time"
                                      },
                                      {
                                        "id": 2,
                                        "name": "SICK_LEAVE",
                                        "defaultDays": 10,
                                        "description": "Sick leave for illness and medical appointments"
                                      },
                                      {
                                        "id": 3,
                                        "name": "FAMILY_RESPONSIBILITY",
                                        "defaultDays": 5,
                                        "description": "Family responsibility leave"
                                      }
                                    ]
                                    """)
                    )),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token")
    })
    public ResponseEntity<List<LeaveTypeResponse>> getAllLeaveTypes() {
        List<LeaveTypeResponse> leaveTypes = leaveTypeService.getAllLeaveTypes();
        return ResponseEntity.ok(leaveTypes);
    }
}

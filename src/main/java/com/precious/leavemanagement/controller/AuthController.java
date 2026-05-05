package com.precious.leavemanagement.controller;

import com.precious.leavemanagement.dto.request.LoginRequest;
import com.precious.leavemanagement.dto.request.RegisterRequest;
import com.precious.leavemanagement.dto.response.AuthResponse;
import com.precious.leavemanagement.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and registration endpoints. No authentication required for these endpoints.")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Create a new user account (EMPLOYEE or MANAGER role). System automatically initializes leave balances for all leave types (Annual: 21 days, Sick: 10 days, Family: 5 days). Returns JWT token for immediate authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully, JWT token returned",
                    content = @Content(
                            schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                      "type": "Bearer",
                                      "user": {
                                        "id": 1,
                                        "email": "john.doe@example.com",
                                        "firstName": "John",
                                        "lastName": "Doe",
                                        "role": "EMPLOYEE",
                                        "enabled": true,
                                        "createdAt": "2026-05-05T10:00:00"
                                      }
                                    }
                                    """)
                    )),
            @ApiResponse(responseCode = "400", description = "Validation error (invalid email, password too short, etc.)",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"message\": \"Email must be valid\"}"))),
            @ApiResponse(responseCode = "409", description = "User with email already exists",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"message\": \"User with email john.doe@example.com already exists\"}")))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User registration details including email, password, name, and role",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = RegisterRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "email": "john.doe@example.com",
                              "password": "SecurePass123",
                              "firstName": "John",
                              "lastName": "Doe",
                              "role": "EMPLOYEE",
                              "department": "Engineering"
                            }
                            """)
            )
    )
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login user",
            description = "Authenticate user with email and password. Returns JWT token valid for 24 hours. Token must be included in Authorization header as 'Bearer {token}' for protected endpoints."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful, JWT token returned",
                    content = @Content(
                            schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                      "type": "Bearer",
                                      "user": {
                                        "id": 1,
                                        "email": "john.doe@example.com",
                                        "firstName": "John",
                                        "lastName": "Doe",
                                        "role": "EMPLOYEE",
                                        "enabled": true,
                                        "createdAt": "2026-05-05T10:00:00"
                                      }
                                    }
                                    """)
                    )),
            @ApiResponse(responseCode = "401", description = "Invalid credentials (wrong email or password)",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"message\": \"Invalid email or password\"}")))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Login credentials with email and password",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = LoginRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "email": "john.doe@example.com",
                              "password": "SecurePass123"
                            }
                            """)
            )
    )
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}

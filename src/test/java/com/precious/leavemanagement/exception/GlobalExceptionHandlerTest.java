package com.precious.leavemanagement.exception;

import com.precious.leavemanagement.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRequestURI("/api/test");
        request = mockRequest;
    }

    @Test
    void handleNotFoundException_ShouldReturn404_ForUserNotFoundException() {
        // Arrange
        UserNotFoundException exception = new UserNotFoundException("User not found");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleNotFoundException(exception, request);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User not found", response.getBody().getMessage());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
        assertEquals("/api/test", response.getBody().getPath());
    }

    @Test
    void handleNotFoundException_ShouldReturn404_ForLeaveRequestNotFoundException() {
        // Arrange
        LeaveRequestNotFoundException exception = new LeaveRequestNotFoundException("Leave request not found");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleNotFoundException(exception, request);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Leave request not found", response.getBody().getMessage());
    }

    @Test
    void handleNotFoundException_ShouldReturn404_ForLeaveTypeNotFoundException() {
        // Arrange
        LeaveTypeNotFoundException exception = new LeaveTypeNotFoundException("Leave type not found");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleNotFoundException(exception, request);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Leave type not found", response.getBody().getMessage());
    }

    @Test
    void handleInsufficientLeaveBalanceException_ShouldReturn400() {
        // Arrange
        InsufficientLeaveBalanceException exception = new InsufficientLeaveBalanceException("Insufficient balance");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInsufficientLeaveBalanceException(exception, request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Insufficient balance", response.getBody().getMessage());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
    }

    @Test
    void handleConflictException_ShouldReturn409_ForOverlappingLeaveRequest() {
        // Arrange
        OverlappingLeaveRequestException exception = new OverlappingLeaveRequestException("Overlapping dates");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleConflictException(exception, request);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Overlapping dates", response.getBody().getMessage());
    }

    @Test
    void handleConflictException_ShouldReturn409_ForLeaveRequestConflict() {
        // Arrange
        LeaveRequestConflictException exception = new LeaveRequestConflictException("Request conflict");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleConflictException(exception, request);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Request conflict", response.getBody().getMessage());
    }

    @Test
    void handleUnauthorizedActionException_ShouldReturn403() {
        // Arrange
        UnauthorizedActionException exception = new UnauthorizedActionException("Unauthorized action");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnauthorizedActionException(exception, request);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Unauthorized action", response.getBody().getMessage());
    }

    @Test
    void handleInvalidLeaveRequestException_ShouldReturn400() {
        // Arrange
        InvalidLeaveRequestException exception = new InvalidLeaveRequestException("Invalid request");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidLeaveRequestException(exception, request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid request", response.getBody().getMessage());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
    }

    @Test
    void handleAccessDeniedException_ShouldReturn403() {
        // Arrange
        AccessDeniedException exception = new AccessDeniedException("Access denied");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDeniedException(exception, request);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Access denied: Access denied", response.getBody().getMessage());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("Forbidden", response.getBody().getError());
    }

    @Test
    void handleInvalidCredentialsException_ShouldReturn401() {
        // Arrange
        InvalidCredentialsException exception = new InvalidCredentialsException("Invalid credentials");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidCredentialsException(exception, request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid credentials", response.getBody().getMessage());
        assertEquals(401, response.getBody().getStatus());
        assertEquals("Unauthorized", response.getBody().getError());
    }

    @Test
    void handleDuplicateResourceException_ShouldReturn409() {
        // Arrange
        DuplicateResourceException exception = new DuplicateResourceException("Resource already exists");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDuplicateResourceException(exception, request);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Resource already exists", response.getBody().getMessage());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Conflict", response.getBody().getError());
    }
}

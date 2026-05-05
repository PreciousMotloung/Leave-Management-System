package com.precious.leavemanagement.service;

import com.precious.leavemanagement.dto.request.LeaveRequestDto;
import com.precious.leavemanagement.dto.request.LeaveReviewRequest;
import com.precious.leavemanagement.dto.response.LeaveRequestResponse;
import com.precious.leavemanagement.entity.LeaveBalance;
import com.precious.leavemanagement.entity.LeaveRequest;
import com.precious.leavemanagement.entity.LeaveType;
import com.precious.leavemanagement.entity.User;
import com.precious.leavemanagement.enums.LeaveStatus;
import com.precious.leavemanagement.enums.LeaveTypeName;
import com.precious.leavemanagement.enums.Role;
import com.precious.leavemanagement.exception.InsufficientLeaveBalanceException;
import com.precious.leavemanagement.exception.InvalidLeaveRequestException;
import com.precious.leavemanagement.exception.LeaveRequestConflictException;
import com.precious.leavemanagement.exception.ResourceNotFoundException;
import com.precious.leavemanagement.exception.UnauthorizedActionException;
import com.precious.leavemanagement.repository.LeaveBalanceRepository;
import com.precious.leavemanagement.repository.LeaveRequestRepository;
import com.precious.leavemanagement.repository.LeaveTypeRepository;
import com.precious.leavemanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LeaveRequestService leaveRequestService;

    private User employee;
    private User manager;
    private LeaveType annualLeave;
    private LeaveBalance leaveBalance;
    private LeaveRequest leaveRequest;

    @BeforeEach
    void setUp() {
        // Setup employee
        employee = User.builder()
                .id(1L)
                .email("employee@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(Role.EMPLOYEE)
                .enabled(true)
                .build();

        // Setup manager
        manager = User.builder()
                .id(2L)
                .email("manager@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .role(Role.MANAGER)
                .enabled(true)
                .build();

        // Setup leave type
        annualLeave = LeaveType.builder()
                .id(1L)
                .name(LeaveTypeName.ANNUAL_LEAVE)
                .defaultDays(21)
                .description("Annual leave")
                .build();

        // Setup leave balance
        leaveBalance = LeaveBalance.builder()
                .id(1L)
                .user(employee)
                .leaveType(annualLeave)
                .availableDays(21)
                .usedDays(0)
                .year(Year.now().getValue())
                .build();

        // Setup leave request
        leaveRequest = LeaveRequest.builder()
                .id(1L)
                .user(employee)
                .leaveType(annualLeave)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .numberOfDays(5)
                .reason("Vacation")
                .status(LeaveStatus.PENDING)
                .build();

        // Mock security context
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void submitLeaveRequest_ShouldCreateRequest_WhenValidRequest() {
        // Arrange
        LeaveRequestDto requestDto = LeaveRequestDto.builder()
                .leaveTypeId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .reason("Vacation")
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("employee@example.com");
        when(userRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(annualLeave));
        when(leaveRequestRepository.findOverlappingLeaveRequests(any(), any(), any()))
                .thenReturn(new ArrayList<>());
        when(leaveBalanceRepository.findByUserAndLeaveTypeAndYear(any(), any(), anyInt()))
                .thenReturn(Optional.of(leaveBalance));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(leaveRequest);

        // Act
        LeaveRequestResponse response = leaveRequestService.submitLeaveRequest(requestDto);

        // Assert
        assertNotNull(response);
        assertEquals(5, response.getNumberOfDays());
        assertEquals(LeaveStatus.PENDING, response.getStatus());
        verify(leaveRequestRepository, times(1)).save(any(LeaveRequest.class));
    }

    @Test
    void submitLeaveRequest_ShouldThrowException_WhenEndDateBeforeStartDate() {
        // Arrange
        LeaveRequestDto requestDto = LeaveRequestDto.builder()
                .leaveTypeId(1L)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(1))  // Invalid: end before start
                .reason("Vacation")
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("employee@example.com");
        when(userRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThrows(InvalidLeaveRequestException.class, 
                () -> leaveRequestService.submitLeaveRequest(requestDto));
    }

    @Test
    void submitLeaveRequest_ShouldThrowException_WhenOverlappingDates() {
        // Arrange
        LeaveRequestDto requestDto = LeaveRequestDto.builder()
                .leaveTypeId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .reason("Vacation")
                .build();

        List<LeaveRequest> overlappingRequests = List.of(leaveRequest);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("employee@example.com");
        when(userRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(annualLeave));
        when(leaveRequestRepository.findOverlappingLeaveRequests(any(), any(), any()))
                .thenReturn(overlappingRequests);

        // Act & Assert
        assertThrows(LeaveRequestConflictException.class, 
                () -> leaveRequestService.submitLeaveRequest(requestDto));
    }

    @Test
    void submitLeaveRequest_ShouldThrowException_WhenInsufficientBalance() {
        // Arrange
        LeaveRequestDto requestDto = LeaveRequestDto.builder()
                .leaveTypeId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(25))  // More than available
                .reason("Vacation")
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("employee@example.com");
        when(userRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(annualLeave));
        when(leaveRequestRepository.findOverlappingLeaveRequests(any(), any(), any()))
                .thenReturn(new ArrayList<>());
        when(leaveBalanceRepository.findByUserAndLeaveTypeAndYear(any(), any(), anyInt()))
                .thenReturn(Optional.of(leaveBalance));

        // Act & Assert
        assertThrows(InsufficientLeaveBalanceException.class, 
                () -> leaveRequestService.submitLeaveRequest(requestDto));
    }

    @Test
    void approveLeave_ShouldDeductBalance_WhenValidRequest() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("manager@example.com");
        when(userRepository.findByEmail("manager@example.com")).thenReturn(Optional.of(manager));
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));
        when(leaveBalanceRepository.findByUserAndLeaveTypeAndYear(any(), any(), anyInt()))
                .thenReturn(Optional.of(leaveBalance));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(leaveRequest);

        // Act
        LeaveRequestResponse response = leaveRequestService.approveLeave(1L);

        // Assert
        assertNotNull(response);
        assertEquals(5, leaveBalance.getUsedDays());
        verify(leaveBalanceRepository, times(1)).save(leaveBalance);
        verify(leaveRequestRepository, times(1)).save(any(LeaveRequest.class));
    }

    @Test
    void approveLeave_ShouldThrowException_WhenApprovingOwnLeave() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("employee@example.com");
        when(userRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));

        // Act & Assert
        assertThrows(UnauthorizedActionException.class, 
                () -> leaveRequestService.approveLeave(1L));
    }

    @Test
    void approveLeave_ShouldThrowException_WhenNotPendingStatus() {
        // Arrange
        leaveRequest.setStatus(LeaveStatus.APPROVED);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("manager@example.com");
        when(userRepository.findByEmail("manager@example.com")).thenReturn(Optional.of(manager));
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));

        // Act & Assert
        assertThrows(InvalidLeaveRequestException.class, 
                () -> leaveRequestService.approveLeave(1L));
    }

    @Test
    void approveLeave_ShouldThrowException_WhenInsufficientBalance() {
        // Arrange
        leaveBalance.setUsedDays(20);  // Only 1 day remaining

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("manager@example.com");
        when(userRepository.findByEmail("manager@example.com")).thenReturn(Optional.of(manager));
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));
        when(leaveBalanceRepository.findByUserAndLeaveTypeAndYear(any(), any(), anyInt()))
                .thenReturn(Optional.of(leaveBalance));

        // Act & Assert
        assertThrows(InsufficientLeaveBalanceException.class, 
                () -> leaveRequestService.approveLeave(1L));
    }

    @Test
    void rejectLeave_ShouldUpdateStatus_WhenValidRequest() {
        // Arrange
        LeaveReviewRequest reviewRequest = LeaveReviewRequest.builder()
                .rejectionReason("Insufficient staffing")
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("manager@example.com");
        when(userRepository.findByEmail("manager@example.com")).thenReturn(Optional.of(manager));
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(leaveRequest);

        // Act
        LeaveRequestResponse response = leaveRequestService.rejectLeave(1L, reviewRequest);

        // Assert
        assertNotNull(response);
        assertEquals(LeaveStatus.REJECTED, leaveRequest.getStatus());
        assertEquals("Insufficient staffing", leaveRequest.getRejectionReason());
        verify(leaveRequestRepository, times(1)).save(any(LeaveRequest.class));
        verify(leaveBalanceRepository, never()).save(any());  // Balance not affected
    }

    @Test
    void cancelLeaveRequest_ShouldRestoreBalance_WhenWasApproved() {
        // Arrange
        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveBalance.setUsedDays(5);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("employee@example.com");
        when(userRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));
        when(leaveBalanceRepository.findByUserAndLeaveTypeAndYear(any(), any(), anyInt()))
                .thenReturn(Optional.of(leaveBalance));

        // Act
        leaveRequestService.cancelLeaveRequest(1L);

        // Assert
        assertEquals(0, leaveBalance.getUsedDays());
        assertEquals(LeaveStatus.CANCELLED, leaveRequest.getStatus());
        verify(leaveBalanceRepository, times(1)).save(leaveBalance);
        verify(leaveRequestRepository, times(1)).save(leaveRequest);
    }

    @Test
    void cancelLeaveRequest_ShouldNotRestoreBalance_WhenWasPending() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("employee@example.com");
        when(userRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));

        // Act
        leaveRequestService.cancelLeaveRequest(1L);

        // Assert
        assertEquals(LeaveStatus.CANCELLED, leaveRequest.getStatus());
        verify(leaveBalanceRepository, never()).save(any());  // Balance not restored
        verify(leaveRequestRepository, times(1)).save(leaveRequest);
    }

    @Test
    void cancelLeaveRequest_ShouldThrowException_WhenNotOwner() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("manager@example.com");
        when(userRepository.findByEmail("manager@example.com")).thenReturn(Optional.of(manager));
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));

        // Act & Assert
        assertThrows(UnauthorizedActionException.class, 
                () -> leaveRequestService.cancelLeaveRequest(1L));
    }

    @Test
    void cancelLeaveRequest_ShouldThrowException_WhenAlreadyCancelled() {
        // Arrange
        leaveRequest.setStatus(LeaveStatus.CANCELLED);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("employee@example.com");
        when(userRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));

        // Act & Assert
        assertThrows(InvalidLeaveRequestException.class, 
                () -> leaveRequestService.cancelLeaveRequest(1L));
    }

    @Test
    void getMyLeaveRequests_ShouldReturnUserRequests() {
        // Arrange
        List<LeaveRequest> requests = List.of(leaveRequest);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("employee@example.com");
        when(userRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findByUser(employee)).thenReturn(requests);

        // Act
        List<LeaveRequestResponse> responses = leaveRequestService.getMyLeaveRequests();

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(leaveRequestRepository, times(1)).findByUser(employee);
    }

    @Test
    void getPendingLeaveRequests_ShouldReturnPendingRequests() {
        // Arrange
        List<LeaveRequest> requests = List.of(leaveRequest);

        when(leaveRequestRepository.findByStatus(LeaveStatus.PENDING)).thenReturn(requests);

        // Act
        List<LeaveRequestResponse> responses = leaveRequestService.getPendingLeaveRequests();

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(leaveRequestRepository, times(1)).findByStatus(LeaveStatus.PENDING);
    }

    @Test
    void getLeaveRequestById_ShouldReturnRequest_WhenOwner() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("employee@example.com");
        when(userRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));

        // Act
        LeaveRequestResponse response = leaveRequestService.getLeaveRequestById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void getLeaveRequestById_ShouldReturnRequest_WhenManager() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("manager@example.com");
        when(userRepository.findByEmail("manager@example.com")).thenReturn(Optional.of(manager));
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));

        // Act
        LeaveRequestResponse response = leaveRequestService.getLeaveRequestById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void getLeaveRequestById_ShouldThrowException_WhenNotOwnerAndNotManager() {
        // Arrange
        User otherEmployee = User.builder()
                .id(3L)
                .email("other@example.com")
                .role(Role.EMPLOYEE)
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("other@example.com");
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherEmployee));
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));

        // Act & Assert
        assertThrows(UnauthorizedActionException.class, 
                () -> leaveRequestService.getLeaveRequestById(1L));
    }

    @Test
    void submitLeaveRequest_ShouldThrowException_WhenLeaveTypeNotFound() {
        // Arrange
        LeaveRequestDto requestDto = LeaveRequestDto.builder()
                .leaveTypeId(999L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("employee@example.com");
        when(userRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
                () -> leaveRequestService.submitLeaveRequest(requestDto));
    }

    @Test
    void approveLeave_ShouldThrowException_WhenRequestNotFound() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("manager@example.com");
        when(userRepository.findByEmail("manager@example.com")).thenReturn(Optional.of(manager));
        when(leaveRequestRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
                () -> leaveRequestService.approveLeave(999L));
    }
}

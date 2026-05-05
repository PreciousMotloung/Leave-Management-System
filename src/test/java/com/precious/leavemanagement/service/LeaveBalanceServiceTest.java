package com.precious.leavemanagement.service;

import com.precious.leavemanagement.dto.response.LeaveBalanceResponse;
import com.precious.leavemanagement.entity.LeaveBalance;
import com.precious.leavemanagement.entity.LeaveType;
import com.precious.leavemanagement.entity.User;
import com.precious.leavemanagement.enums.LeaveTypeName;
import com.precious.leavemanagement.enums.Role;
import com.precious.leavemanagement.exception.ResourceNotFoundException;
import com.precious.leavemanagement.repository.LeaveBalanceRepository;
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

import java.time.Year;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveBalanceServiceTest {

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LeaveBalanceService leaveBalanceService;

    private User user;
    private LeaveType annualLeave;
    private LeaveType sickLeave;
    private LeaveBalance annualBalance;
    private LeaveBalance sickBalance;

    @BeforeEach
    void setUp() {
        // Setup user
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(Role.EMPLOYEE)
                .enabled(true)
                .build();

        // Setup leave types
        annualLeave = LeaveType.builder()
                .id(1L)
                .name(LeaveTypeName.ANNUAL_LEAVE)
                .defaultDays(21)
                .description("Annual leave")
                .build();

        sickLeave = LeaveType.builder()
                .id(2L)
                .name(LeaveTypeName.SICK_LEAVE)
                .defaultDays(10)
                .description("Sick leave")
                .build();

        // Setup leave balances
        annualBalance = LeaveBalance.builder()
                .id(1L)
                .user(user)
                .leaveType(annualLeave)
                .availableDays(21)
                .usedDays(5)
                .year(Year.now().getValue())
                .build();

        sickBalance = LeaveBalance.builder()
                .id(2L)
                .user(user)
                .leaveType(sickLeave)
                .availableDays(10)
                .usedDays(2)
                .year(Year.now().getValue())
                .build();

        // Mock security context
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getMyLeaveBalances_ShouldReturnBalances_WhenUserExists() {
        // Arrange
        List<LeaveBalance> balances = List.of(annualBalance, sickBalance);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(leaveBalanceRepository.findByUserAndYear(user, Year.now().getValue()))
                .thenReturn(balances);

        // Act
        List<LeaveBalanceResponse> responses = leaveBalanceService.getMyLeaveBalances();

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        
        LeaveBalanceResponse annualResponse = responses.get(0);
        assertEquals(LeaveTypeName.ANNUAL_LEAVE, annualResponse.getLeaveType().getName());
        assertEquals(21, annualResponse.getAvailableDays());
        assertEquals(5, annualResponse.getUsedDays());
        assertEquals(16, annualResponse.getRemainingDays());

        LeaveBalanceResponse sickResponse = responses.get(1);
        assertEquals(LeaveTypeName.SICK_LEAVE, sickResponse.getLeaveType().getName());
        assertEquals(10, sickResponse.getAvailableDays());
        assertEquals(2, sickResponse.getUsedDays());
        assertEquals(8, sickResponse.getRemainingDays());

        verify(leaveBalanceRepository, times(1))
                .findByUserAndYear(user, Year.now().getValue());
    }

    @Test
    void getMyLeaveBalances_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("nonexistent@example.com");
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> leaveBalanceService.getMyLeaveBalances()
        );

        assertTrue(exception.getMessage().contains("User not found"));
        verify(leaveBalanceRepository, never()).findByUserAndYear(any(), anyInt());
    }

    @Test
    void getUserLeaveBalances_ShouldReturnBalances_WhenUserExists() {
        // Arrange
        List<LeaveBalance> balances = List.of(annualBalance, sickBalance);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(leaveBalanceRepository.findByUserAndYear(user, Year.now().getValue()))
                .thenReturn(balances);

        // Act
        List<LeaveBalanceResponse> responses = leaveBalanceService.getUserLeaveBalances(1L);

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        verify(userRepository, times(1)).findById(1L);
        verify(leaveBalanceRepository, times(1))
                .findByUserAndYear(user, Year.now().getValue());
    }

    @Test
    void getUserLeaveBalances_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> leaveBalanceService.getUserLeaveBalances(999L)
        );

        assertTrue(exception.getMessage().contains("User not found"));
        verify(userRepository, times(1)).findById(999L);
        verify(leaveBalanceRepository, never()).findByUserAndYear(any(), anyInt());
    }

    @Test
    void getUserLeaveBalances_ShouldReturnEmptyList_WhenNoBalances() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(leaveBalanceRepository.findByUserAndYear(user, Year.now().getValue()))
                .thenReturn(List.of());

        // Act
        List<LeaveBalanceResponse> responses = leaveBalanceService.getUserLeaveBalances(1L);

        // Assert
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
        verify(leaveBalanceRepository, times(1))
                .findByUserAndYear(user, Year.now().getValue());
    }

    @Test
    void getMyLeaveBalances_ShouldCalculateRemainingDaysCorrectly() {
        // Arrange
        List<LeaveBalance> balances = List.of(annualBalance);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(leaveBalanceRepository.findByUserAndYear(user, Year.now().getValue()))
                .thenReturn(balances);

        // Act
        List<LeaveBalanceResponse> responses = leaveBalanceService.getMyLeaveBalances();

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        
        LeaveBalanceResponse response = responses.get(0);
        assertEquals(21, response.getAvailableDays());
        assertEquals(5, response.getUsedDays());
        assertEquals(16, response.getRemainingDays());  // 21 - 5 = 16
    }

    @Test
    void getUserLeaveBalances_ShouldReturnBalancesForSpecificYear() {
        // Arrange
        int currentYear = Year.now().getValue();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(leaveBalanceRepository.findByUserAndYear(user, currentYear))
                .thenReturn(List.of(annualBalance));

        // Act
        List<LeaveBalanceResponse> responses = leaveBalanceService.getUserLeaveBalances(1L);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(leaveBalanceRepository, times(1)).findByUserAndYear(user, currentYear);
    }

    @Test
    void getMyLeaveBalances_ShouldMapLeaveTypeCorrectly() {
        // Arrange
        List<LeaveBalance> balances = List.of(annualBalance);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(leaveBalanceRepository.findByUserAndYear(user, Year.now().getValue()))
                .thenReturn(balances);

        // Act
        List<LeaveBalanceResponse> responses = leaveBalanceService.getMyLeaveBalances();

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        
        LeaveBalanceResponse response = responses.get(0);
        assertNotNull(response.getLeaveType());
        assertEquals(annualLeave.getId(), response.getLeaveType().getId());
        assertEquals(annualLeave.getName(), response.getLeaveType().getName());
        assertEquals(annualLeave.getDefaultDays(), response.getLeaveType().getDefaultDays());
        assertEquals(annualLeave.getDescription(), response.getLeaveType().getDescription());
    }
}

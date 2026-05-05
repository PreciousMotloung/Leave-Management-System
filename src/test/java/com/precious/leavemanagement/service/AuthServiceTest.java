package com.precious.leavemanagement.service;

import com.precious.leavemanagement.dto.request.LoginRequest;
import com.precious.leavemanagement.dto.request.RegisterRequest;
import com.precious.leavemanagement.dto.response.AuthResponse;
import com.precious.leavemanagement.entity.LeaveBalance;
import com.precious.leavemanagement.entity.LeaveType;
import com.precious.leavemanagement.entity.User;
import com.precious.leavemanagement.enums.LeaveTypeName;
import com.precious.leavemanagement.enums.Role;
import com.precious.leavemanagement.exception.DuplicateResourceException;
import com.precious.leavemanagement.exception.InvalidCredentialsException;
import com.precious.leavemanagement.repository.LeaveBalanceRepository;
import com.precious.leavemanagement.repository.LeaveTypeRepository;
import com.precious.leavemanagement.repository.UserRepository;
import com.precious.leavemanagement.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Year;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private User user;
    private LeaveType annualLeave;
    private LeaveType sickLeave;
    private LeaveType familyResponsibility;

    @BeforeEach
    void setUp() {
        // Setup user
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPassword")
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

        familyResponsibility = LeaveType.builder()
                .id(3L)
                .name(LeaveTypeName.FAMILY_RESPONSIBILITY)
                .defaultDays(5)
                .description("Family responsibility leave")
                .build();
    }

    @Test
    void register_ShouldCreateUserAndLeaveBalances_WhenValidRequest() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@example.com")
                .password("password123")
                .firstName("Jane")
                .lastName("Smith")
                .role(Role.EMPLOYEE)
                .build();

        List<LeaveType> leaveTypes = List.of(annualLeave, sickLeave, familyResponsibility);

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(leaveTypeRepository.findAll()).thenReturn(leaveTypes);
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-token");

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertNotNull(response.getUser());
        assertEquals("test@example.com", response.getUser().getEmail());

        verify(userRepository, times(1)).existsByEmail("newuser@example.com");
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(any(User.class));
        verify(leaveBalanceRepository, times(3)).save(any(LeaveBalance.class));
        verify(jwtUtil, times(1)).generateToken(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenEmailAlreadyExists() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@example.com")
                .password("password123")
                .firstName("Jane")
                .lastName("Smith")
                .role(Role.EMPLOYEE)
                .build();

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> authService.register(request)
        );

        assertEquals("User with email existing@example.com already exists", exception.getMessage());
        verify(userRepository, times(1)).existsByEmail("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
        verify(leaveBalanceRepository, never()).save(any(LeaveBalance.class));
    }

    @Test
    void register_ShouldCreateManagerUser_WhenRoleIsManager() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("manager@example.com")
                .password("password123")
                .firstName("Jane")
                .lastName("Manager")
                .role(Role.MANAGER)
                .build();

        User manager = User.builder()
                .id(2L)
                .email("manager@example.com")
                .password("encodedPassword")
                .firstName("Jane")
                .lastName("Manager")
                .role(Role.MANAGER)
                .enabled(true)
                .build();

        List<LeaveType> leaveTypes = List.of(annualLeave, sickLeave, familyResponsibility);

        when(userRepository.existsByEmail("manager@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(manager);
        when(leaveTypeRepository.findAll()).thenReturn(leaveTypes);
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-token");

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals(Role.MANAGER, response.getUser().getRole());
        verify(leaveBalanceRepository, times(3)).save(any(LeaveBalance.class));
    }

    @Test
    void register_ShouldCreateLeaveBalancesWithCorrectDefaults() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@example.com")
                .password("password123")
                .firstName("Jane")
                .lastName("Smith")
                .role(Role.EMPLOYEE)
                .build();

        List<LeaveType> leaveTypes = List.of(annualLeave, sickLeave, familyResponsibility);

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(leaveTypeRepository.findAll()).thenReturn(leaveTypes);
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenAnswer(invocation -> {
            LeaveBalance balance = invocation.getArgument(0);
            
            // Verify the balance has correct properties
            assertNotNull(balance.getUser());
            assertNotNull(balance.getLeaveType());
            assertEquals(balance.getLeaveType().getDefaultDays(), balance.getAvailableDays());
            assertEquals(0, balance.getUsedDays());
            assertEquals(Year.now().getValue(), balance.getYear());
            
            return balance;
        });
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-token");

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        verify(leaveBalanceRepository, times(3)).save(any(LeaveBalance.class));
    }

    @Test
    void login_ShouldReturnAuthResponse_WhenCredentialsAreValid() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-token");

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertNotNull(response.getUser());
        assertEquals("test@example.com", response.getUser().getEmail());
        assertEquals("John", response.getUser().getFirstName());
        assertEquals("Doe", response.getUser().getLastName());
        assertEquals(Role.EMPLOYEE, response.getUser().getRole());

        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(jwtUtil, times(1)).generateToken(any(User.class));
    }

    @Test
    void login_ShouldThrowException_WhenCredentialsAreInvalid() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("wrongpassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());
        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByEmail(anyString());
        verify(jwtUtil, never()).generateToken(any(User.class));
    }

    @Test
    void login_ShouldThrowException_WhenUserNotFoundAfterAuthentication() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.login(request)
        );

        assertTrue(exception.getMessage().contains("User not found"));
        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(jwtUtil, never()).generateToken(any(User.class));
    }

    @Test
    void register_ShouldSetEnabledToTrue() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@example.com")
                .password("password123")
                .firstName("Jane")
                .lastName("Smith")
                .role(Role.EMPLOYEE)
                .build();

        List<LeaveType> leaveTypes = List.of(annualLeave, sickLeave, familyResponsibility);

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertTrue(savedUser.isEnabled());
            return savedUser;
        });
        when(leaveTypeRepository.findAll()).thenReturn(leaveTypes);
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-token");

        // Act
        authService.register(request);

        // Assert
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_ShouldEncodePassword() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@example.com")
                .password("plainPassword")
                .firstName("Jane")
                .lastName("Smith")
                .role(Role.EMPLOYEE)
                .build();

        List<LeaveType> leaveTypes = List.of(annualLeave, sickLeave, familyResponsibility);

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertEquals("encodedPassword", savedUser.getPassword());
            return savedUser;
        });
        when(leaveTypeRepository.findAll()).thenReturn(leaveTypes);
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-token");

        // Act
        authService.register(request);

        // Assert
        verify(passwordEncoder, times(1)).encode("plainPassword");
        verify(userRepository, times(1)).save(any(User.class));
    }
}

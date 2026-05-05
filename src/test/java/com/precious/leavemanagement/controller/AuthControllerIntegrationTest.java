package com.precious.leavemanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.precious.leavemanagement.dto.request.LoginRequest;
import com.precious.leavemanagement.dto.request.RegisterRequest;
import com.precious.leavemanagement.entity.LeaveType;
import com.precious.leavemanagement.entity.User;
import com.precious.leavemanagement.enums.LeaveTypeName;
import com.precious.leavemanagement.enums.Role;
import com.precious.leavemanagement.repository.LeaveBalanceRepository;
import com.precious.leavemanagement.repository.LeaveRequestRepository;
import com.precious.leavemanagement.repository.LeaveTypeRepository;
import com.precious.leavemanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        leaveRequestRepository.deleteAll();
        leaveBalanceRepository.deleteAll();
        userRepository.deleteAll();
        leaveTypeRepository.deleteAll();

        // Create leave types
        LeaveType annual = LeaveType.builder()
                .name(LeaveTypeName.ANNUAL_LEAVE)
                .defaultDays(20)
                .build();
        LeaveType sick = LeaveType.builder()
                .name(LeaveTypeName.SICK_LEAVE)
                .defaultDays(10)
                .build();
        LeaveType family = LeaveType.builder()
                .name(LeaveTypeName.FAMILY_RESPONSIBILITY)
                .defaultDays(5)
                .build();

        leaveTypeRepository.save(annual);
        leaveTypeRepository.save(sick);
        leaveTypeRepository.save(family);
    }

    @Test
    void register_ShouldReturn201_WhenValidRequest() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@test.com")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .role(Role.EMPLOYEE)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value("newuser@test.com"))
                .andExpect(jsonPath("$.user.firstName").value("New"))
                .andExpect(jsonPath("$.user.lastName").value("User"))
                .andExpect(jsonPath("$.user.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.user.enabled").value(true));
    }

    @Test
    void register_ShouldReturn409_WhenEmailAlreadyExists() throws Exception {
        // Arrange
        User existingUser = User.builder()
                .email("existing@test.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("Existing")
                .lastName("User")
                .role(Role.EMPLOYEE)
                .enabled(true)
                .build();
        userRepository.save(existingUser);

        RegisterRequest request = RegisterRequest.builder()
                .email("existing@test.com")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .role(Role.EMPLOYEE)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    void login_ShouldReturn200_WhenCredentialsValid() throws Exception {
        // Arrange
        User user = User.builder()
                .email("user@test.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("Test")
                .lastName("User")
                .role(Role.EMPLOYEE)
                .enabled(true)
                .build();
        userRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .email("user@test.com")
                .password("password123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value("user@test.com"));
    }

    @Test
    void login_ShouldReturn401_WhenCredentialsInvalid() throws Exception {
        // Arrange
        User user = User.builder()
                .email("user@test.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("Test")
                .lastName("User")
                .role(Role.EMPLOYEE)
                .enabled(true)
                .build();
        userRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .email("user@test.com")
                .password("wrongpassword")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("Invalid")));
    }

    @Test
    void login_ShouldReturn401_WhenUserNotFound() throws Exception {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@test.com")
                .password("password123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("Invalid")));
    }
}

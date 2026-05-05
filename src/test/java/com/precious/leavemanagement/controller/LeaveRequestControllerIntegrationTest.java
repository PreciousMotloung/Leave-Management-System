package com.precious.leavemanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.precious.leavemanagement.dto.request.LeaveRequestDto;
import com.precious.leavemanagement.dto.request.LeaveReviewRequest;
import com.precious.leavemanagement.dto.request.LoginRequest;
import com.precious.leavemanagement.dto.request.RegisterRequest;
import com.precious.leavemanagement.dto.response.AuthResponse;
import com.precious.leavemanagement.entity.LeaveBalance;
import com.precious.leavemanagement.entity.LeaveRequest;
import com.precious.leavemanagement.entity.LeaveType;
import com.precious.leavemanagement.entity.User;
import com.precious.leavemanagement.enums.LeaveStatus;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LeaveRequestControllerIntegrationTest {

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

    private User employee;
    private User manager;
    private LeaveType annualLeave;
    private LeaveType sickLeave;
    private String employeeToken;
    private String managerToken;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up
        leaveRequestRepository.deleteAll();
        leaveBalanceRepository.deleteAll();
        userRepository.deleteAll();
        leaveTypeRepository.deleteAll();

        // Create leave types
        annualLeave = leaveTypeRepository.save(LeaveType.builder()
                .name(LeaveTypeName.ANNUAL_LEAVE)
                .defaultDays(21)
                .description("Annual leave")
                .build());

        sickLeave = leaveTypeRepository.save(LeaveType.builder()
                .name(LeaveTypeName.SICK_LEAVE)
                .defaultDays(10)
                .description("Sick leave")
                .build());

        leaveTypeRepository.save(LeaveType.builder()
                .name(LeaveTypeName.FAMILY_RESPONSIBILITY)
                .defaultDays(5)
                .description("Family responsibility")
                .build());

        // Create employee
        employee = userRepository.save(User.builder()
                .email("employee@test.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("John")
                .lastName("Doe")
                .role(Role.EMPLOYEE)
                .enabled(true)
                .build());

        // Create manager
        manager = userRepository.save(User.builder()
                .email("manager@test.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("Jane")
                .lastName("Manager")
                .role(Role.MANAGER)
                .enabled(true)
                .build());

        // Create leave balances for employee
        leaveBalanceRepository.save(LeaveBalance.builder()
                .user(employee)
                .leaveType(annualLeave)
                .availableDays(21)
                .usedDays(0)
                .year(Year.now().getValue())
                .build());

        leaveBalanceRepository.save(LeaveBalance.builder()
                .user(employee)
                .leaveType(sickLeave)
                .availableDays(10)
                .usedDays(0)
                .year(Year.now().getValue())
                .build());

        // Get tokens
        employeeToken = getAuthToken("employee@test.com", "password123");
        managerToken = getAuthToken("manager@test.com", "password123");
    }

    private String getAuthToken(String email, String password) throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthResponse.class
        );

        return authResponse.getToken();
    }

    @Test
    void submitLeaveRequest_ShouldReturn201_WhenValidRequest() throws Exception {
        LeaveRequestDto request = LeaveRequestDto.builder()
                .leaveTypeId(annualLeave.getId())
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .reason("Vacation")
                .build();

        mockMvc.perform(post("/api/leave/submit")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.numberOfDays").value(5))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.reason").value("Vacation"));
    }

    @Test
    void submitLeaveRequest_ShouldReturn400_WhenInsufficientBalance() throws Exception {
        LeaveRequestDto request = LeaveRequestDto.builder()
                .leaveTypeId(annualLeave.getId())
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(25))  // More than 21 days available
                .reason("Long vacation")
                .build();

        mockMvc.perform(post("/api/leave/submit")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Insufficient leave balance")));
    }

    @Test
    void submitLeaveRequest_ShouldReturn409_WhenDatesOverlap() throws Exception {
        // Submit first request
        LeaveRequest existingRequest = leaveRequestRepository.save(LeaveRequest.builder()
                .user(employee)
                .leaveType(annualLeave)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .numberOfDays(5)
                .reason("First request")
                .status(LeaveStatus.PENDING)
                .build());

        // Try to submit overlapping request
        LeaveRequestDto request = LeaveRequestDto.builder()
                .leaveTypeId(annualLeave.getId())
                .startDate(LocalDate.now().plusDays(3))  // Overlaps with existing
                .endDate(LocalDate.now().plusDays(7))
                .reason("Second request")
                .build();

        mockMvc.perform(post("/api/leave/submit")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("already have leave scheduled")));
    }

    @Test
    void approveLeave_ShouldReturn200AndDeductBalance_WhenManager() throws Exception {
        // Create pending leave request
        LeaveRequest leaveRequest = leaveRequestRepository.save(LeaveRequest.builder()
                .user(employee)
                .leaveType(annualLeave)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .numberOfDays(5)
                .reason("Vacation")
                .status(LeaveStatus.PENDING)
                .build());

        // Get initial balance
        LeaveBalance balanceBefore = leaveBalanceRepository
                .findByUserAndLeaveTypeAndYear(employee, annualLeave, Year.now().getValue())
                .orElseThrow();
        int initialUsedDays = balanceBefore.getUsedDays();

        // Approve as manager
        mockMvc.perform(put("/api/leave/" + leaveRequest.getId() + "/approve")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedBy").exists());

        // Verify balance was deducted
        LeaveBalance balanceAfter = leaveBalanceRepository
                .findByUserAndLeaveTypeAndYear(employee, annualLeave, Year.now().getValue())
                .orElseThrow();

        assert balanceAfter.getUsedDays() == initialUsedDays + 5;
        assert balanceAfter.getRemainingDays() == 16;  // 21 - 5
    }

    @Test
    void approveLeave_ShouldReturn403_WhenEmployee() throws Exception {
        // Create pending leave request
        LeaveRequest leaveRequest = leaveRequestRepository.save(LeaveRequest.builder()
                .user(employee)
                .leaveType(annualLeave)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .numberOfDays(5)
                .reason("Vacation")
                .status(LeaveStatus.PENDING)
                .build());

        // Try to approve as employee (should fail)
        mockMvc.perform(put("/api/leave/" + leaveRequest.getId() + "/approve")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelLeaveRequest_ShouldReturn200AndRestoreBalance_WhenWasApproved() throws Exception {
        // Create approved leave request with balance deducted
        LeaveBalance balance = leaveBalanceRepository
                .findByUserAndLeaveTypeAndYear(employee, annualLeave, Year.now().getValue())
                .orElseThrow();
        balance.setUsedDays(5);
        leaveBalanceRepository.save(balance);

        LeaveRequest leaveRequest = leaveRequestRepository.save(LeaveRequest.builder()
                .user(employee)
                .leaveType(annualLeave)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .numberOfDays(5)
                .reason("Vacation")
                .status(LeaveStatus.APPROVED)
                .build());

        // Cancel as employee
        mockMvc.perform(put("/api/leave/" + leaveRequest.getId() + "/cancel")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());

        // Verify balance was restored
        LeaveBalance balanceAfter = leaveBalanceRepository
                .findByUserAndLeaveTypeAndYear(employee, annualLeave, Year.now().getValue())
                .orElseThrow();

        assert balanceAfter.getUsedDays() == 0;  // Restored
        assert balanceAfter.getRemainingDays() == 21;
    }

    @Test
    void rejectLeave_ShouldReturn200_WhenManager() throws Exception {
        LeaveRequest leaveRequest = leaveRequestRepository.save(LeaveRequest.builder()
                .user(employee)
                .leaveType(annualLeave)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .numberOfDays(5)
                .reason("Vacation")
                .status(LeaveStatus.PENDING)
                .build());

        LeaveReviewRequest reviewRequest = LeaveReviewRequest.builder()
                .rejectionReason("Insufficient staffing")
                .build();

        mockMvc.perform(put("/api/leave/" + leaveRequest.getId() + "/reject")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Insufficient staffing"));
    }

    @Test
    void getMyLeaveRequests_ShouldReturn200_WithOwnRequests() throws Exception {
        leaveRequestRepository.save(LeaveRequest.builder()
                .user(employee)
                .leaveType(annualLeave)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .numberOfDays(5)
                .reason("Vacation")
                .status(LeaveStatus.PENDING)
                .build());

        mockMvc.perform(get("/api/leave/my-requests")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].reason").value("Vacation"));
    }

    @Test
    void getPendingLeaveRequests_ShouldReturn200_WhenManager() throws Exception {
        leaveRequestRepository.save(LeaveRequest.builder()
                .user(employee)
                .leaveType(annualLeave)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .numberOfDays(5)
                .reason("Vacation")
                .status(LeaveStatus.PENDING)
                .build());

        mockMvc.perform(get("/api/leave/pending")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void getPendingLeaveRequests_ShouldReturn403_WhenEmployee() throws Exception {
        mockMvc.perform(get("/api/leave/pending")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyLeaveBalances_ShouldReturn200_WithBalances() throws Exception {
        mockMvc.perform(get("/api/leave/balance")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].availableDays").value(21))
                .andExpect(jsonPath("$[0].usedDays").value(0))
                .andExpect(jsonPath("$[0].remainingDays").value(21));
    }

    @Test
    void getUserLeaveBalances_ShouldReturn200_WhenManager() throws Exception {
        mockMvc.perform(get("/api/leave/balance/" + employee.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getUserLeaveBalances_ShouldReturn403_WhenEmployee() throws Exception {
        mockMvc.perform(get("/api/leave/balance/" + employee.getId())
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllLeaveTypes_ShouldReturn200_WhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/leave/types")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].defaultDays").exists());
    }

    @Test
    void submitLeaveRequest_ShouldReturn401_WhenNoToken() throws Exception {
        LeaveRequestDto request = LeaveRequestDto.builder()
                .leaveTypeId(annualLeave.getId())
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .reason("Vacation")
                .build();

        mockMvc.perform(post("/api/leave/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_ShouldReturn201AndInitializeBalances() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@test.com")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .role(Role.EMPLOYEE)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value("newuser@test.com"))
                .andReturn();

        // Verify leave balances were created
        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthResponse.class
        );

        User newUser = userRepository.findById(authResponse.getUser().getId()).orElseThrow();
        long balanceCount = leaveBalanceRepository.findByUserAndYear(newUser, Year.now().getValue()).size();
        assert balanceCount == 3;  // All 3 leave types
    }
}

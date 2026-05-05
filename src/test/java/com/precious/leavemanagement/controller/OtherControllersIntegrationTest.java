package com.precious.leavemanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.precious.leavemanagement.entity.LeaveBalance;
import com.precious.leavemanagement.entity.LeaveType;
import com.precious.leavemanagement.entity.User;
import com.precious.leavemanagement.enums.LeaveTypeName;
import com.precious.leavemanagement.enums.Role;
import com.precious.leavemanagement.repository.LeaveBalanceRepository;
import com.precious.leavemanagement.repository.LeaveRequestRepository;
import com.precious.leavemanagement.repository.LeaveTypeRepository;
import com.precious.leavemanagement.repository.UserRepository;
import com.precious.leavemanagement.security.JwtUtil;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OtherControllersIntegrationTest {

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

    @Autowired
    private JwtUtil jwtUtil;

    private String employeeToken;
    private String managerToken;
    private User employee;
    private User manager;
    private LeaveType annualLeave;

    @BeforeEach
    void setUp() {
        leaveRequestRepository.deleteAll();
        leaveBalanceRepository.deleteAll();
        userRepository.deleteAll();
        leaveTypeRepository.deleteAll();

        // Create leave types
        annualLeave = LeaveType.builder()
                .name(LeaveTypeName.ANNUAL_LEAVE)
                .defaultDays(20)
                .build();
        LeaveType sickLeave = LeaveType.builder()
                .name(LeaveTypeName.SICK_LEAVE)
                .defaultDays(10)
                .build();

        annualLeave = leaveTypeRepository.save(annualLeave);
        sickLeave = leaveTypeRepository.save(sickLeave);

        // Create users
        employee = User.builder()
                .email("employee@test.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("John")
                .lastName("Doe")
                .role(Role.EMPLOYEE)
                .enabled(true)
                .build();
        employee = userRepository.save(employee);

        manager = User.builder()
                .email("manager@test.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("Jane")
                .lastName("Manager")
                .role(Role.MANAGER)
                .enabled(true)
                .build();
        manager = userRepository.save(manager);

        // Create leave balances
        LeaveBalance employeeBalance = LeaveBalance.builder()
                .user(employee)
                .leaveType(annualLeave)
                .availableDays(20)
                .usedDays(5)
                .year(LocalDate.now().getYear())
                .build();
        leaveBalanceRepository.save(employeeBalance);

        // Generate tokens
        employeeToken = jwtUtil.generateToken(employee);
        managerToken = jwtUtil.generateToken(manager);
    }

    // ========== LeaveBalanceController Tests ==========

    @Test
    void getMyLeaveBalances_ShouldReturn200_WhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/leave/balance")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].leaveType.name").value("ANNUAL_LEAVE"))
                .andExpect(jsonPath("$[0].availableDays").value(20))
                .andExpect(jsonPath("$[0].usedDays").value(5))
                .andExpect(jsonPath("$[0].remainingDays").value(15));
    }

    @Test
    void getMyLeaveBalances_ShouldReturn403_WhenNoToken() throws Exception {
        mockMvc.perform(get("/api/leave/balance"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserLeaveBalances_ShouldReturn200_WhenManager() throws Exception {
        mockMvc.perform(get("/api/leave/balance/" + employee.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].leaveType.name").value("ANNUAL_LEAVE"));
    }

    @Test
    void getUserLeaveBalances_ShouldReturn403_WhenEmployee() throws Exception {
        mockMvc.perform(get("/api/leave/balance/" + manager.getId())
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    // ========== LeaveTypeController Tests ==========

    @Test
    void getAllLeaveTypes_ShouldReturn200_WhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/leave/types")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("ANNUAL_LEAVE"))
                .andExpect(jsonPath("$[0].defaultDays").value(20))
                .andExpect(jsonPath("$[1].name").value("SICK_LEAVE"))
                .andExpect(jsonPath("$[1].defaultDays").value(10));
    }

    @Test
    void getAllLeaveTypes_ShouldReturn403_WhenNoToken() throws Exception {
        mockMvc.perform(get("/api/leave/types"))
                .andExpect(status().isForbidden());
    }
}

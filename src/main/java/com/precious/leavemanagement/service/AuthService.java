package com.precious.leavemanagement.service;

import com.precious.leavemanagement.dto.request.LoginRequest;
import com.precious.leavemanagement.dto.request.RegisterRequest;
import com.precious.leavemanagement.dto.response.AuthResponse;
import com.precious.leavemanagement.dto.response.UserResponse;
import com.precious.leavemanagement.entity.LeaveBalance;
import com.precious.leavemanagement.entity.LeaveType;
import com.precious.leavemanagement.entity.User;
import com.precious.leavemanagement.exception.DuplicateResourceException;
import com.precious.leavemanagement.repository.LeaveBalanceRepository;
import com.precious.leavemanagement.repository.LeaveTypeRepository;
import com.precious.leavemanagement.repository.UserRepository;
import com.precious.leavemanagement.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                      LeaveTypeRepository leaveTypeRepository,
                      LeaveBalanceRepository leaveBalanceRepository,
                      PasswordEncoder passwordEncoder,
                      JwtUtil jwtUtil,
                      AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User with email " + request.getEmail() + " already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole())
                .department(request.getDepartment())
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        // Create leave balances for all leave types
        List<LeaveType> leaveTypes = leaveTypeRepository.findAll();
        int currentYear = Year.now().getValue();

        for (LeaveType leaveType : leaveTypes) {
            LeaveBalance balance = LeaveBalance.builder()
                    .user(savedUser)
                    .leaveType(leaveType)
                    .availableDays(leaveType.getDefaultDays())
                    .usedDays(0)
                    .year(currentYear)
                    .build();
            leaveBalanceRepository.save(balance);
        }

        String token = jwtUtil.generateToken(savedUser);
        UserResponse userResponse = mapToUserResponse(savedUser);

        return new AuthResponse(token, userResponse);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtil.generateToken(user);
        UserResponse userResponse = mapToUserResponse(user);

        return new AuthResponse(token, userResponse);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

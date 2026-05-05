package com.precious.leavemanagement.service;

import com.precious.leavemanagement.dto.response.LeaveBalanceResponse;
import com.precious.leavemanagement.dto.response.LeaveTypeResponse;
import com.precious.leavemanagement.entity.LeaveBalance;
import com.precious.leavemanagement.entity.User;
import com.precious.leavemanagement.exception.ResourceNotFoundException;
import com.precious.leavemanagement.repository.LeaveBalanceRepository;
import com.precious.leavemanagement.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final UserRepository userRepository;

    public LeaveBalanceService(LeaveBalanceRepository leaveBalanceRepository,
                              UserRepository userRepository) {
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.userRepository = userRepository;
    }

    public List<LeaveBalanceResponse> getMyLeaveBalances() {
        User currentUser = getCurrentUser();
        int currentYear = Year.now().getValue();
        
        List<LeaveBalance> balances = leaveBalanceRepository.findByUserAndYear(currentUser, currentYear);
        return balances.stream()
                .map(this::mapToLeaveBalanceResponse)
                .collect(Collectors.toList());
    }

    public List<LeaveBalanceResponse> getUserLeaveBalances(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        int currentYear = Year.now().getValue();
        List<LeaveBalance> balances = leaveBalanceRepository.findByUserAndYear(user, currentYear);
        
        return balances.stream()
                .map(this::mapToLeaveBalanceResponse)
                .collect(Collectors.toList());
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private LeaveBalanceResponse mapToLeaveBalanceResponse(LeaveBalance balance) {
        LeaveTypeResponse leaveType = LeaveTypeResponse.builder()
                .id(balance.getLeaveType().getId())
                .name(balance.getLeaveType().getName())
                .defaultDays(balance.getLeaveType().getDefaultDays())
                .description(balance.getLeaveType().getDescription())
                .build();

        return LeaveBalanceResponse.builder()
                .id(balance.getId())
                .leaveType(leaveType)
                .availableDays(balance.getAvailableDays())
                .usedDays(balance.getUsedDays())
                .remainingDays(balance.getRemainingDays())
                .year(balance.getYear())
                .build();
    }
}

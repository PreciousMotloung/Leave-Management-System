package com.precious.leavemanagement.service;

import com.precious.leavemanagement.dto.request.LeaveRequestDto;
import com.precious.leavemanagement.dto.request.LeaveReviewRequest;
import com.precious.leavemanagement.dto.response.LeaveRequestResponse;
import com.precious.leavemanagement.dto.response.LeaveTypeResponse;
import com.precious.leavemanagement.dto.response.UserResponse;
import com.precious.leavemanagement.entity.LeaveBalance;
import com.precious.leavemanagement.entity.LeaveRequest;
import com.precious.leavemanagement.entity.LeaveType;
import com.precious.leavemanagement.entity.User;
import com.precious.leavemanagement.enums.LeaveStatus;
import com.precious.leavemanagement.enums.Role;
import com.precious.leavemanagement.exception.*;
import com.precious.leavemanagement.repository.LeaveBalanceRepository;
import com.precious.leavemanagement.repository.LeaveRequestRepository;
import com.precious.leavemanagement.repository.LeaveTypeRepository;
import com.precious.leavemanagement.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final UserRepository userRepository;

    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository,
                              LeaveTypeRepository leaveTypeRepository,
                              LeaveBalanceRepository leaveBalanceRepository,
                              UserRepository userRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public LeaveRequestResponse submitLeaveRequest(LeaveRequestDto request) {
        User currentUser = getCurrentUser();
        
        // Validate dates
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new InvalidLeaveRequestException("End date must be after or equal to start date");
        }

        // Calculate number of days
        long numberOfDays = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;

        // Get leave type
        LeaveType leaveType = leaveTypeRepository.findById(request.getLeaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found"));

        // Check for overlapping leave requests
        List<LeaveRequest> overlappingRequests = leaveRequestRepository.findOverlappingLeaveRequests(
                currentUser, request.getStartDate(), request.getEndDate());
        
        if (!overlappingRequests.isEmpty()) {
            throw new LeaveRequestConflictException("You already have leave scheduled during this period");
        }

        // Check leave balance
        int currentYear = Year.now().getValue();
        LeaveBalance balance = leaveBalanceRepository.findByUserAndLeaveTypeAndYear(
                        currentUser, leaveType, currentYear)
                .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found"));

        int remainingDays = balance.getAvailableDays() - balance.getUsedDays();
        if (remainingDays < numberOfDays) {
            throw new InsufficientLeaveBalanceException(
                    "Insufficient leave balance. Available: " + remainingDays + " days, Requested: " + numberOfDays + " days");
        }

        // Create leave request
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .user(currentUser)
                .leaveType(leaveType)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .numberOfDays((int) numberOfDays)
                .reason(request.getReason())
                .status(LeaveStatus.PENDING)
                .build();

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        return mapToLeaveRequestResponse(savedRequest);
    }

    @Transactional
    public LeaveRequestResponse approveLeave(Long requestId) {
        User currentUser = getCurrentUser();
        
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        // Manager cannot approve their own leave
        if (leaveRequest.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException("You cannot approve your own leave request");
        }

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidLeaveRequestException("Only pending requests can be approved");
        }

        // Update leave balance
        int currentYear = Year.now().getValue();
        LeaveBalance balance = leaveBalanceRepository.findByUserAndLeaveTypeAndYear(
                        leaveRequest.getUser(), leaveRequest.getLeaveType(), currentYear)
                .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found"));

        int remainingDays = balance.getAvailableDays() - balance.getUsedDays();
        if (remainingDays < leaveRequest.getNumberOfDays()) {
            throw new InsufficientLeaveBalanceException(
                    "Cannot approve: Insufficient leave balance. Available: " + remainingDays + 
                    " days, Requested: " + leaveRequest.getNumberOfDays() + " days");
        }

        // Deduct from balance
        balance.setUsedDays(balance.getUsedDays() + leaveRequest.getNumberOfDays());
        leaveBalanceRepository.save(balance);

        // Update request status
        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setApprovedBy(currentUser);
        leaveRequest.setApprovedAt(LocalDateTime.now());

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        return mapToLeaveRequestResponse(savedRequest);
    }

    @Transactional
    public LeaveRequestResponse rejectLeave(Long requestId, LeaveReviewRequest reviewRequest) {
        User currentUser = getCurrentUser();
        
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidLeaveRequestException("Only pending requests can be rejected");
        }

        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setApprovedBy(currentUser);
        leaveRequest.setApprovedAt(LocalDateTime.now());
        leaveRequest.setRejectionReason(reviewRequest.getRejectionReason());

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        return mapToLeaveRequestResponse(savedRequest);
    }

    @Transactional
    public void cancelLeaveRequest(Long requestId) {
        User currentUser = getCurrentUser();
        
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        // Employee can only cancel their own request
        if (!leaveRequest.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException("You can only cancel your own leave requests");
        }

        if (leaveRequest.getStatus() == LeaveStatus.CANCELLED || leaveRequest.getStatus() == LeaveStatus.REJECTED) {
            throw new InvalidLeaveRequestException("This request has already been " + leaveRequest.getStatus().name().toLowerCase());
        }

        // If the request was approved, restore the balance
        if (leaveRequest.getStatus() == LeaveStatus.APPROVED) {
            int currentYear = Year.now().getValue();
            LeaveBalance balance = leaveBalanceRepository.findByUserAndLeaveTypeAndYear(
                            currentUser, leaveRequest.getLeaveType(), currentYear)
                    .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found"));

            balance.setUsedDays(balance.getUsedDays() - leaveRequest.getNumberOfDays());
            leaveBalanceRepository.save(balance);
        }

        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        leaveRequestRepository.save(leaveRequest);
    }

    public List<LeaveRequestResponse> getMyLeaveRequests() {
        User currentUser = getCurrentUser();
        List<LeaveRequest> requests = leaveRequestRepository.findByUser(currentUser);
        return requests.stream()
                .map(this::mapToLeaveRequestResponse)
                .collect(Collectors.toList());
    }

    public List<LeaveRequestResponse> getPendingLeaveRequests() {
        List<LeaveRequest> requests = leaveRequestRepository.findByStatus(LeaveStatus.PENDING);
        return requests.stream()
                .map(this::mapToLeaveRequestResponse)
                .collect(Collectors.toList());
    }

    public LeaveRequestResponse getLeaveRequestById(Long id) {
        User currentUser = getCurrentUser();
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        // Employee can only view their own requests, manager can view all
        if (currentUser.getRole() != Role.MANAGER && 
            !leaveRequest.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException("You can only view your own leave requests");
        }

        return mapToLeaveRequestResponse(leaveRequest);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private LeaveRequestResponse mapToLeaveRequestResponse(LeaveRequest request) {
        return LeaveRequestResponse.builder()
                .id(request.getId())
                .user(mapToUserResponse(request.getUser()))
                .leaveType(mapToLeaveTypeResponse(request.getLeaveType()))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .numberOfDays(request.getNumberOfDays())
                .reason(request.getReason())
                .status(request.getStatus())
                .approvedBy(request.getApprovedBy() != null ? mapToUserResponse(request.getApprovedBy()) : null)
                .approvedAt(request.getApprovedAt())
                .rejectionReason(request.getRejectionReason())
                .createdAt(request.getCreatedAt())
                .build();
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

    private LeaveTypeResponse mapToLeaveTypeResponse(LeaveType leaveType) {
        return LeaveTypeResponse.builder()
                .id(leaveType.getId())
                .name(leaveType.getName())
                .defaultDays(leaveType.getDefaultDays())
                .description(leaveType.getDescription())
                .build();
    }
}

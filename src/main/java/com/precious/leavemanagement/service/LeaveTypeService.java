package com.precious.leavemanagement.service;

import com.precious.leavemanagement.dto.response.LeaveTypeResponse;
import com.precious.leavemanagement.entity.LeaveType;
import com.precious.leavemanagement.repository.LeaveTypeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;

    public LeaveTypeService(LeaveTypeRepository leaveTypeRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
    }

    public List<LeaveTypeResponse> getAllLeaveTypes() {
        List<LeaveType> leaveTypes = leaveTypeRepository.findAll();
        return leaveTypes.stream()
                .map(this::mapToLeaveTypeResponse)
                .collect(Collectors.toList());
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

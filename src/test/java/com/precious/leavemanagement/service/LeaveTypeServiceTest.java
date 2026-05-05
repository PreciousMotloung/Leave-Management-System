package com.precious.leavemanagement.service;

import com.precious.leavemanagement.dto.response.LeaveTypeResponse;
import com.precious.leavemanagement.entity.LeaveType;
import com.precious.leavemanagement.enums.LeaveTypeName;
import com.precious.leavemanagement.repository.LeaveTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveTypeServiceTest {

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @InjectMocks
    private LeaveTypeService leaveTypeService;

    private LeaveType annualLeave;
    private LeaveType sickLeave;
    private LeaveType familyResponsibility;

    @BeforeEach
    void setUp() {
        annualLeave = LeaveType.builder()
                .id(1L)
                .name(LeaveTypeName.ANNUAL_LEAVE)
                .defaultDays(21)
                .description("Annual leave for vacation and personal time")
                .build();

        sickLeave = LeaveType.builder()
                .id(2L)
                .name(LeaveTypeName.SICK_LEAVE)
                .defaultDays(10)
                .description("Sick leave for illness and medical appointments")
                .build();

        familyResponsibility = LeaveType.builder()
                .id(3L)
                .name(LeaveTypeName.FAMILY_RESPONSIBILITY)
                .defaultDays(5)
                .description("Family responsibility leave")
                .build();
    }

    @Test
    void getAllLeaveTypes_ShouldReturnAllTypes() {
        // Arrange
        List<LeaveType> leaveTypes = List.of(annualLeave, sickLeave, familyResponsibility);
        when(leaveTypeRepository.findAll()).thenReturn(leaveTypes);

        // Act
        List<LeaveTypeResponse> responses = leaveTypeService.getAllLeaveTypes();

        // Assert
        assertNotNull(responses);
        assertEquals(3, responses.size());

        LeaveTypeResponse annualResponse = responses.get(0);
        assertEquals(1L, annualResponse.getId());
        assertEquals(LeaveTypeName.ANNUAL_LEAVE, annualResponse.getName());
        assertEquals(21, annualResponse.getDefaultDays());
        assertEquals("Annual leave for vacation and personal time", annualResponse.getDescription());

        LeaveTypeResponse sickResponse = responses.get(1);
        assertEquals(2L, sickResponse.getId());
        assertEquals(LeaveTypeName.SICK_LEAVE, sickResponse.getName());
        assertEquals(10, sickResponse.getDefaultDays());

        LeaveTypeResponse familyResponse = responses.get(2);
        assertEquals(3L, familyResponse.getId());
        assertEquals(LeaveTypeName.FAMILY_RESPONSIBILITY, familyResponse.getName());
        assertEquals(5, familyResponse.getDefaultDays());

        verify(leaveTypeRepository, times(1)).findAll();
    }

    @Test
    void getAllLeaveTypes_ShouldReturnEmptyList_WhenNoTypesExist() {
        // Arrange
        when(leaveTypeRepository.findAll()).thenReturn(List.of());

        // Act
        List<LeaveTypeResponse> responses = leaveTypeService.getAllLeaveTypes();

        // Assert
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
        verify(leaveTypeRepository, times(1)).findAll();
    }

    @Test
    void getAllLeaveTypes_ShouldMapAllFieldsCorrectly() {
        // Arrange
        List<LeaveType> leaveTypes = List.of(annualLeave);
        when(leaveTypeRepository.findAll()).thenReturn(leaveTypes);

        // Act
        List<LeaveTypeResponse> responses = leaveTypeService.getAllLeaveTypes();

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        
        LeaveTypeResponse response = responses.get(0);
        assertEquals(annualLeave.getId(), response.getId());
        assertEquals(annualLeave.getName(), response.getName());
        assertEquals(annualLeave.getDefaultDays(), response.getDefaultDays());
        assertEquals(annualLeave.getDescription(), response.getDescription());
    }

    @Test
    void getAllLeaveTypes_ShouldReturnCorrectOrder() {
        // Arrange
        List<LeaveType> leaveTypes = List.of(sickLeave, annualLeave, familyResponsibility);
        when(leaveTypeRepository.findAll()).thenReturn(leaveTypes);

        // Act
        List<LeaveTypeResponse> responses = leaveTypeService.getAllLeaveTypes();

        // Assert
        assertNotNull(responses);
        assertEquals(3, responses.size());
        
        // Verify order is preserved
        assertEquals(LeaveTypeName.SICK_LEAVE, responses.get(0).getName());
        assertEquals(LeaveTypeName.ANNUAL_LEAVE, responses.get(1).getName());
        assertEquals(LeaveTypeName.FAMILY_RESPONSIBILITY, responses.get(2).getName());
    }

    @Test
    void getAllLeaveTypes_ShouldHandleSingleType() {
        // Arrange
        List<LeaveType> leaveTypes = List.of(annualLeave);
        when(leaveTypeRepository.findAll()).thenReturn(leaveTypes);

        // Act
        List<LeaveTypeResponse> responses = leaveTypeService.getAllLeaveTypes();

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(LeaveTypeName.ANNUAL_LEAVE, responses.get(0).getName());
        verify(leaveTypeRepository, times(1)).findAll();
    }
}

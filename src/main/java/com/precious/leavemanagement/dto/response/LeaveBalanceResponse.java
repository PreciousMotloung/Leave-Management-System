package com.precious.leavemanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalanceResponse {
    private Long id;
    private LeaveTypeResponse leaveType;
    private Integer availableDays;
    private Integer usedDays;
    private Integer remainingDays;
    private Integer year;
}

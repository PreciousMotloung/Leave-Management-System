package com.precious.leavemanagement.dto.response;

import com.precious.leavemanagement.enums.LeaveTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveTypeResponse {
    private Long id;
    private LeaveTypeName name;
    private Integer defaultDays;
    private String description;
}

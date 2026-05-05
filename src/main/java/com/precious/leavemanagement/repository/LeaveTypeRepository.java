package com.precious.leavemanagement.repository;

import com.precious.leavemanagement.entity.LeaveType;
import com.precious.leavemanagement.enums.LeaveTypeName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {
    Optional<LeaveType> findByName(LeaveTypeName name);
}

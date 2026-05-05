package com.precious.leavemanagement.repository;

import com.precious.leavemanagement.entity.LeaveBalance;
import com.precious.leavemanagement.entity.LeaveType;
import com.precious.leavemanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    List<LeaveBalance> findByUser(User user);
    List<LeaveBalance> findByUserAndYear(User user, Integer year);
    Optional<LeaveBalance> findByUserAndLeaveType(User user, LeaveType leaveType);
    Optional<LeaveBalance> findByUserAndLeaveTypeAndYear(User user, LeaveType leaveType, Integer year);
}

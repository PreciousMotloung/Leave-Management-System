package com.precious.leavemanagement.repository;

import com.precious.leavemanagement.entity.LeaveRequest;
import com.precious.leavemanagement.entity.User;
import com.precious.leavemanagement.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByUser(User user);
    List<LeaveRequest> findByStatus(LeaveStatus status);
    List<LeaveRequest> findByUserAndStatus(User user, LeaveStatus status);
    
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.user = :user " +
           "AND lr.status IN ('PENDING', 'APPROVED') " +
           "AND ((lr.startDate <= :endDate AND lr.endDate >= :startDate))")
    List<LeaveRequest> findOverlappingLeaveRequests(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}

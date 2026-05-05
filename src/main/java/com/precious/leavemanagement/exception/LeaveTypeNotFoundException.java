package com.precious.leavemanagement.exception;

public class LeaveTypeNotFoundException extends RuntimeException {
    public LeaveTypeNotFoundException(String message) {
        super(message);
    }
}

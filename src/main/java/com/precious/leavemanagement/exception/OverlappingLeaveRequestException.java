package com.precious.leavemanagement.exception;

public class OverlappingLeaveRequestException extends RuntimeException {
    public OverlappingLeaveRequestException(String message) {
        super(message);
    }
}

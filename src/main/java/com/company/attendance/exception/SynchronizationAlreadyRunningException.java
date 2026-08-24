package com.company.attendance.exception;

public class SynchronizationAlreadyRunningException extends RuntimeException {

    public SynchronizationAlreadyRunningException(String message) {
        super(message);
    }
}

package com.company.attendance.security.exception;

import org.springframework.security.core.AuthenticationException;

public class InvalidRefreshTokenException extends AuthenticationException {

    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token");
    }
}

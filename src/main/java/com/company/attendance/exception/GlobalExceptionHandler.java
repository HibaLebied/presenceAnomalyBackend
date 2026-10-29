package com.company.attendance.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<String> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .toList();

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<String> details = exception.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();

        return buildResponse(HttpStatus.BAD_REQUEST, "Constraint violation", request, details);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(SynchronizationAlreadyRunningException.class)
    public ResponseEntity<ApiErrorResponse> handleSynchronizationAlreadyRunningException(
            SynchronizationAlreadyRunningException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(SynchronizationException.class)
    public ResponseEntity<ApiErrorResponse> handleSynchronizationException(
            SynchronizationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(),
                request,
                List.of(exception.getCause().getClass().getSimpleName())
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request,
                List.of()
        );
    }


    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Authentication failed",
                request,
                List.of(exception.getClass().getSimpleName())
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Access denied",
                request,
                List.of()
        );
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected server error",
                request,
                List.of(exception.getClass().getSimpleName())
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            List<String> details
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                details
        );

        return ResponseEntity.status(status).body(response);
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}

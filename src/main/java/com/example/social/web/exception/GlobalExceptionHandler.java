package com.example.social.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ApiError> badRequest(Exception e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage(), request);
    }

    @ExceptionHandler({UnauthorizedException.class})
    public ResponseEntity<ApiError> unauthorized(Exception e, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", e.getMessage(), request);
    }

    @ExceptionHandler({ForbiddenException.class})
    public ResponseEntity<ApiError> forbidden(Exception e, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", e.getMessage(), request);
    }

    @ExceptionHandler({NotFoundException.class})
    public ResponseEntity<ApiError> notFound(Exception e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage(), request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message, HttpServletRequest request) {
        var body = ApiError.builder()
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .status(status.value())
                .code(code)
                .message(message)
                .build();

        return ResponseEntity.status(status).body(body);
    }
}

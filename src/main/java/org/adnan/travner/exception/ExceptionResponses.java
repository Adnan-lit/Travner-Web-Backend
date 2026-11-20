package org.adnan.travner.exception;

import org.adnan.travner.config.CorrelationIdFilter;
import org.adnan.travner.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;

/**
 * Single source of truth for API error responses. Both {@link GlobalExceptionHandler}
 * and controller catch blocks delegate here so every error carries the same
 * stable code, safe message, and correlation ID. Internal exception messages are
 * logged with the full stack trace but never returned to the client on a 5xx.
 *
 * The methods are generic in the payload type so callers returning
 * {@code ResponseEntity<ApiResponse<String>>} or raw {@code ResponseEntity<?>}
 * can delegate without casts.
 */
public final class ExceptionResponses {

    public static final String INTERNAL_MESSAGE = "An unexpected error occurred. Please try again later.";
    private static final Logger log = LoggerFactory.getLogger(ExceptionResponses.class);

    private ExceptionResponses() {
    }

    /** Build an error response with an explicit code/status/message. */
    public static <T> ResponseEntity<ApiResponse<T>> of(ErrorCode code, HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(code.value(), message, traceId()));
    }

    /** Build an error response carrying extra data (e.g. per-field validation errors). */
    public static <T> ResponseEntity<ApiResponse<T>> of(ErrorCode code, HttpStatus status, String message, T data) {
        ApiResponse<T> body = ApiResponse.<T>builder()
                .success(false)
                .code(code.value())
                .message(message)
                .traceId(traceId())
                .data(data)
                .build();
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Map any exception to a safe response. 4xx messages may echo client-derived
     * validation text; 5xx always uses a fixed generic message.
     */
    public static <T> ResponseEntity<ApiResponse<T>> from(Throwable ex) {
        Throwable cause = ex;

        if (cause instanceof ForbiddenException || cause instanceof AccessDeniedException) {
            log.warn("Authorization denied [trace={}]: {}", traceId(), cause.getMessage());
            return of(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Access denied");
        }
        if (cause instanceof BadCredentialsException || cause instanceof UsernameNotFoundException) {
            log.warn("Invalid credentials [trace={}]", traceId());
            return of(ErrorCode.AUTH_INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        if (cause instanceof DuplicateUserException) {
            log.warn("Duplicate registration attempted [trace={}]", traceId());
            return of(ErrorCode.DUPLICATE_USER, HttpStatus.CONFLICT, "Username or email already exists");
        }
        if (cause instanceof InvalidOrderStateException) {
            log.warn("Invalid order transition [trace={}]: {}", traceId(), cause.getMessage());
            return of(ErrorCode.ORDER_INVALID_STATE, HttpStatus.CONFLICT,
                    "Order cannot be changed from its current status");
        }
        if (cause instanceof OptimisticLockingFailureException) {
            log.warn("Concurrent modification [trace={}]", traceId());
            return of(ErrorCode.CONFLICT, HttpStatus.CONFLICT,
                    "Order was modified concurrently; reload and retry");
        }
        if (cause instanceof ResourceNotFoundException) {
            log.warn("Resource not found [trace={}]: {}", traceId(), cause.getMessage());
            return of(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Resource not found");
        }
        if (cause instanceof MethodArgumentTypeMismatchException) {
            MethodArgumentTypeMismatchException mismatch = (MethodArgumentTypeMismatchException) cause;
            log.warn("Type mismatch [trace={}] for parameter {}", traceId(), mismatch.getName());
            return of(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                    "Invalid value for parameter '" + mismatch.getName() + "'");
        }
        if (cause instanceof IllegalArgumentException
                || cause instanceof MethodArgumentNotValidException
                || cause instanceof BindException
                || cause instanceof jakarta.validation.ConstraintViolationException) {
            log.warn("Validation failed [trace={}]: {}", traceId(), cause.getMessage());
            return of(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, safeClientMessage(cause));
        }

        // Anything else is a server error: log with stack trace, return a fixed message.
        log.error("Unhandled exception [trace={}]", traceId(), cause);
        return of(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_MESSAGE);
    }

    private static String safeClientMessage(Throwable ex) {
        String message = ex.getMessage();
        return (message == null || message.isBlank()) ? "Invalid request" : message;
    }

    private static String traceId() {
        String id = MDC.get(CorrelationIdFilter.MDC_KEY);
        return (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id;
    }
}

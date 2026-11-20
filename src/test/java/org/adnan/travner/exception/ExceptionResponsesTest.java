package org.adnan.travner.exception;

import org.adnan.travner.dto.ApiResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExceptionResponsesTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    private ApiResponse<Object> body(Throwable ex) {
        return ExceptionResponses.from(ex).getBody();
    }

    @Test
    void forbiddenMapsTo403WithStableCodeAndGenericMessage() {
        ResponseEntity<ApiResponse<Object>> response = ExceptionResponses.from(
                new ForbiddenException("secret ownership reason"));
        ApiResponse<Object> body = response.getBody();

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ErrorCode.FORBIDDEN.value(), body.getCode());
        assertEquals("Access denied", body.getMessage());
        assertFalse(body.toString().contains("secret ownership reason"));
        assertNotNull(body.getTraceId());
    }

    @Test
    void accessDeniedMapsTo403() {
        ApiResponse<Object> body = body(new AccessDeniedException("denied"));
        assertEquals(ErrorCode.FORBIDDEN.value(), body.getCode());
        assertEquals("Access denied", body.getMessage());
    }

    @Test
    void badCredentialsAndUsernameNotFoundMapTo401() {
        assertEquals(ErrorCode.AUTH_INVALID_CREDENTIALS.value(),
                body(new BadCredentialsException("bad")).getCode());
        assertEquals(ErrorCode.AUTH_INVALID_CREDENTIALS.value(),
                body(new UsernameNotFoundException("missing")).getCode());
    }

    @Test
    void resourceNotFoundMapsTo404() {
        ResponseEntity<ApiResponse<Object>> response =
                ExceptionResponses.from(new ResourceNotFoundException("Product not found: 123"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.value(), response.getBody().getCode());
        assertFalse(response.getBody().getMessage().contains("123"));
    }

    @Test
    void duplicateUserMapsTo409() {
        assertEquals(ErrorCode.DUPLICATE_USER.value(), body(new DuplicateUserException()).getCode());
    }

    @Test
    void orderStateAndOptimisticLockMapTo409() {
        assertEquals(ErrorCode.ORDER_INVALID_STATE.value(),
                body(new InvalidOrderStateException("PAID -> DRAFT")).getCode());
        assertEquals(ErrorCode.CONFLICT.value(),
                body(new OptimisticLockingFailureException("version")).getCode());
    }

    @Test
    void illegalArgumentMapsTo400WithClientMessage() {
        ResponseEntity<ApiResponse<Object>> response =
                ExceptionResponses.from(new IllegalArgumentException("page must be zero or greater"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorCode.VALIDATION_FAILED.value(), response.getBody().getCode());
        assertEquals("page must be zero or greater", response.getBody().getMessage());
    }

    @Test
    void unexpectedExceptionNeverLeaksInternalMessage() {
        ResponseEntity<ApiResponse<Object>> response =
                ExceptionResponses.from(new IOException("mongodb://user:password@internal-host:27017"));
        ApiResponse<Object> body = response.getBody();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(ErrorCode.INTERNAL_ERROR.value(), body.getCode());
        assertEquals(ExceptionResponses.INTERNAL_MESSAGE, body.getMessage());
        assertFalse(body.toString().contains("internal-host"));
        assertFalse(body.toString().contains("password"));
    }

    @Test
    void traceIdIsReadFromMdc() {
        MDC.put("correlationId", "req-123-abc");
        assertEquals("req-123-abc", body(new IllegalArgumentException("x")).getTraceId());
    }

    @Test
    void typeMismatchDoesNotEchoRawValue() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "secret-value", Integer.class, "page", null, null);
        ApiResponse<Object> body = body(ex);
        assertEquals(HttpStatus.BAD_REQUEST, ExceptionResponses.from(ex).getStatusCode());
        assertFalse(body.getMessage().contains("secret-value"));
        assertTrue(body.getMessage().contains("page"));
    }
}

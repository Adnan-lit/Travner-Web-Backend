package org.adnan.travner.exception;

import org.adnan.travner.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ForbiddenExceptionHandlerTest {

    @Test
    void authorizationFailureMapsTo403WithGenericMessage() {
        ResponseEntity<ApiResponse<Object>> response =
                new GlobalExceptionHandler().handleForbidden(new ForbiddenException("secret internal reason"));

        assertEquals(403, response.getStatusCode().value());
        assertEquals("FORBIDDEN", response.getBody().getCode());
        assertEquals("Access denied", response.getBody().getMessage());
        assertFalse(response.getBody().toString().contains("secret internal reason"));
    }

    @Test
    void methodSecurityDenialMapsTo403() {
        ResponseEntity<ApiResponse<Object>> response =
                new GlobalExceptionHandler().handleAccessDenied(new AccessDeniedException("denied"));

        assertEquals(403, response.getStatusCode().value());
        assertEquals("FORBIDDEN", response.getBody().getCode());
        assertEquals("Access denied", response.getBody().getMessage());
    }
}

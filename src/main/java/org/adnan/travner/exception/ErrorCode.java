package org.adnan.travner.exception;

/**
 * Stable, machine-readable codes returned in {@code ApiResponse.code}.
 * Clients may branch on these; the human-readable message may change.
 */
public enum ErrorCode {
    VALIDATION_FAILED("VALIDATION_FAILED"),
    INVALID_REQUEST("INVALID_REQUEST"),
    INVALID_ID("INVALID_ID"),
    AUTH_REQUIRED("AUTH_REQUIRED"),
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS"),
    FORBIDDEN("FORBIDDEN"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND"),
    ORDER_NOT_FOUND("ORDER_NOT_FOUND"),
    ORDER_INVALID_STATE("ORDER_INVALID_STATE"),
    CONFLICT("CONFLICT"),
    DUPLICATE_USER("DUPLICATE_USER"),
    RATE_LIMITED("RATE_LIMITED"),
    INTERNAL_ERROR("INTERNAL_ERROR");

    private final String value;

    ErrorCode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}

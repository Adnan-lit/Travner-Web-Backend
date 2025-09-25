package org.adnan.travner.exception;

/**
 * Exception thrown when a requested resource is not found
 * This should result in 404 Not Found instead of 400 Bad Request
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
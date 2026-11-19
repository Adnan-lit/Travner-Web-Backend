package org.adnan.travner.exception;

/** Thrown when an authenticated principal lacks object-level permission. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}

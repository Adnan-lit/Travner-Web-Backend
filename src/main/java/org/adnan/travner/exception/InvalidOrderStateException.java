package org.adnan.travner.exception;

/**
 * An attempted order status transition is not permitted by the state machine.
 * Mapped to 409 CONFLICT; callers receive a stable message, not internal state.
 */
public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(String message) {
        super(message);
    }
}

package org.adnan.travner.exception;

/**
 * A signup collided with an existing username or email. The public message is
 * deliberately generic so callers cannot enumerate which identifier exists.
 */
public class DuplicateUserException extends RuntimeException {

    public DuplicateUserException() {
        super("Username or email already exists");
    }
}

package org.adnan.travner.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the P2-10 decision to remove password reset entirely: the dead
 * in-memory token flow must not be reintroduced without revisiting the decision.
 */
class PasswordResetRemovalTest {

    @Test
    void userServiceDoesNotExposePasswordResetTokenFlow() {
        boolean hasResetApi = Arrays.stream(UserService.class.getDeclaredMethods())
                .map(Method::getName)
                .anyMatch(name -> name.contains("PasswordReset")
                        || name.contains("ResetToken")
                        || name.equals("hashToken"));

        assertTrue(hasResetApi == false,
                "Password-reset token methods must stay removed per P2-10");
    }
}

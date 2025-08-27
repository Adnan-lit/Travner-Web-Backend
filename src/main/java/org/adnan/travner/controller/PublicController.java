package org.adnan.travner.controller;

import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("public")
public class PublicController {

    @Autowired
    private UserService userService;

    @PostMapping("create-user")
    public ResponseEntity<Void> createUser(@RequestBody UserEntry user) {
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            userService.saveNewUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check username availability
     * GET /public/check-username/{username}
     */
    @GetMapping("check-username/{username}")
    public ResponseEntity<Map<String, Object>> checkUsernameAvailability(@PathVariable String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createResponse("error", "Username is required", null));
            }

            // Validate username format (basic validation)
            if (!isValidUsername(username)) {
                return ResponseEntity.badRequest()
                        .body(createResponse("error", "Invalid username format", false));
            }

            boolean available = userService.isUsernameAvailable(username.trim());
            String message = available ? "Username is available" : "Username is already taken";

            return ResponseEntity.ok(
                    createResponse("message", message, available));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createResponse("error", "Internal server error", null));
        }
    }

    /**
     * Helper method to create response maps
     */
    private Map<String, Object> createResponse(String messageKey, String message, Boolean available) {
        Map<String, Object> response = new HashMap<>();
        response.put(messageKey, message);
        if (available != null) {
            response.put("available", available);
        }
        return response;
    }

    /**
     * Basic username validation
     */
    private boolean isValidUsername(String username) {
        // Username should be 3-50 characters, alphanumeric and underscore only
        return username != null &&
                username.matches("^[a-zA-Z0-9_]{3,50}$");
    }

    /**
     * Request password reset
     * POST /public/forgot-password
     */
    @PostMapping("forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createResponse("error", "Username is required", null));
            }

            // Generate reset token (this will return null if user doesn't exist, for security)
            String token = userService.generatePasswordResetToken(username.trim());

            // Always return success to prevent username enumeration
            String message = "If the username exists, a password reset token has been generated. " +
                    "In production, this would be sent via email.";

            Map<String, Object> response = createResponse("message", message, null);
            if (token != null) {
                // In production, don't return the token - send it via email instead
                response.put("resetToken", token); // Only for testing purposes
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createResponse("error", "Internal server error", null));
        }
    }

    /**
     * Reset password with token
     * POST /public/reset-password
     */
    @PostMapping("reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            String newPassword = request.get("newPassword");

            if (token == null || token.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createResponse("error", "Reset token is required", null));
            }

            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createResponse("error", "New password is required", null));
            }

            if (newPassword.length() < 6) {
                return ResponseEntity.badRequest()
                        .body(createResponse("error", "Password must be at least 6 characters long", null));
            }

            boolean success = userService.resetPasswordWithToken(token.trim(), newPassword);
            if (!success) {
                return ResponseEntity.badRequest()
                        .body(createResponse("error", "Invalid or expired reset token", null));
            }

            return ResponseEntity.ok(
                    createResponse("message", "Password reset successfully", null)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createResponse("error", "Internal server error", null));
        }
    }
}
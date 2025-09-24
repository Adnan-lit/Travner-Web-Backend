package org.adnan.travner.controller;

import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({ "/user", "/api/users" })
public class UserController {

    @Autowired
    private UserService userService;

    private String getAuthenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return auth.getName();
    }

    @GetMapping
    public ResponseEntity<Object> login() {
        String username = getAuthenticatedUsername();
        if (username == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        UserEntry user = userService.getByUsernameSecure(username);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @DeleteMapping
    public ResponseEntity<Object> deleteUser() {
        String username = getAuthenticatedUsername();
        if (username == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        UserEntry user = userService.getByUsername(username);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        boolean deleted = userService.deleteUser(user);
        if (!deleted) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Get user profile
     * GET /user/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<Object> getProfile() {
        String username = getAuthenticatedUsername();
        if (username == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        UserEntry user = userService.getByUsernameSecure(username);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    /**
     * Update user profile (full update)
     * PUT /user/profile
     */
    @PutMapping("/profile")
    public ResponseEntity<Map<String, String>> updateProfile(@RequestBody Map<String, String> profileData) {
        String username = getAuthenticatedUsername();
        if (username == null) {
            return new ResponseEntity<>(createResponse("error", "Authentication required"), HttpStatus.UNAUTHORIZED);
        }

        try {
            String firstName = profileData.get("firstName");
            String lastName = profileData.get("lastName");
            String email = profileData.get("email");
            String bio = profileData.get("bio");
            String location = profileData.get("location");

            // Validate email format if provided
            if (email != null && !email.trim().isEmpty() && !isValidEmail(email)) {
                return new ResponseEntity<>(createResponse("error", "Invalid email format"), HttpStatus.BAD_REQUEST);
            }

            boolean updated = userService.updateUserProfileEnhanced(username, firstName, lastName, email, bio,
                    location);
            if (!updated) {
                return new ResponseEntity<>(createResponse("error", "Failed to update profile"),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }

            return new ResponseEntity<>(createResponse("message", "Profile updated successfully"), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(createResponse("error", "Internal server error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Partially update user profile
     * PATCH /user/profile
     */
    @PatchMapping("/profile")
    public ResponseEntity<Map<String, String>> updateProfilePartial(@RequestBody Map<String, Object> updates) {
        String username = getAuthenticatedUsername();
        if (username == null) {
            return new ResponseEntity<>(createResponse("error", "Authentication required"), HttpStatus.UNAUTHORIZED);
        }

        try {
            // Validate email if present
            if (updates.containsKey("email")) {
                Object emailObj = updates.get("email");
                if (emailObj instanceof String email && !email.trim().isEmpty() && !isValidEmail(email)) {
                    return new ResponseEntity<>(createResponse("error", "Invalid email format"),
                            HttpStatus.BAD_REQUEST);
                }
            }

            // Prevent updating sensitive fields
            updates.remove("password");
            updates.remove("userName");
            updates.remove("roles");
            updates.remove("id");

            if (updates.isEmpty()) {
                return new ResponseEntity<>(createResponse("error", "No valid fields to update"),
                        HttpStatus.BAD_REQUEST);
            }

            boolean updated = userService.updateUserPartial(username, updates);
            if (!updated) {
                return new ResponseEntity<>(createResponse("error", "Failed to update profile"),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }

            return new ResponseEntity<>(createResponse("message", "Profile updated successfully"), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(createResponse("error", "Internal server error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Change user password
     * PUT /user/password
     */
    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> passwordData) {
        String username = getAuthenticatedUsername();
        if (username == null) {
            return new ResponseEntity<>(createResponse("error", "Authentication required"), HttpStatus.UNAUTHORIZED);
        }

        try {
            String currentPassword = passwordData.get("currentPassword");
            String newPassword = passwordData.get("newPassword");

            // Validate input
            if (currentPassword == null || currentPassword.trim().isEmpty()) {
                return new ResponseEntity<>(createResponse("error", "Current password is required"),
                        HttpStatus.BAD_REQUEST);
            }

            if (newPassword == null || newPassword.trim().isEmpty()) {
                return new ResponseEntity<>(createResponse("error", "New password is required"),
                        HttpStatus.BAD_REQUEST);
            }

            // Follow password security requirements: minimum 6 characters
            if (newPassword.length() < 6) {
                return new ResponseEntity<>(createResponse("error", "Password must be at least 6 characters long"),
                        HttpStatus.BAD_REQUEST);
            }

            boolean changed = userService.changeUserPassword(username, currentPassword, newPassword);
            if (!changed) {
                return new ResponseEntity<>(
                        createResponse("error", "Current password is incorrect or failed to update password"),
                        HttpStatus.BAD_REQUEST);
            }

            return new ResponseEntity<>(createResponse("message", "Password changed successfully"), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(createResponse("error", "Internal server error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get public user profile by username
     * GET /user/public/{username}
     */
    @GetMapping("/public/{username}")
    public ResponseEntity<Object> getPublicProfile(@PathVariable String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return new ResponseEntity<>(createResponse("error", "Username is required"), HttpStatus.BAD_REQUEST);
            }

            UserEntry user = userService.getByUsernameSecure(username.trim());
            if (user == null) {
                return new ResponseEntity<>(createResponse("error", "User not found"), HttpStatus.NOT_FOUND);
            }

            // Create public profile response (exclude sensitive data)
            Map<String, Object> publicProfile = new HashMap<>();
            publicProfile.put("userName", user.getUserName());
            publicProfile.put("firstName", user.getFirstName());
            publicProfile.put("lastName", user.getLastName());
            publicProfile.put("bio", user.getBio());
            publicProfile.put("location", user.getLocation());
            publicProfile.put("profileImageUrl", user.getProfileImageUrl());
            publicProfile.put("createdAt", user.getCreatedAt());

            return new ResponseEntity<>(publicProfile, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(createResponse("error", "Internal server error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Helper method to create response maps
     */
    private Map<String, String> createResponse(String key, String value) {
        Map<String, String> response = new HashMap<>();
        response.put(key, value);
        return response;
    }

    /**
     * Basic email validation
     */
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
package org.adnan.travner.controller;

import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.dto.UserSummaryDTO;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Get current user profile
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserSummaryDTO>> getUserProfile(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Authentication required"));
            }

            String username = authentication.getName();
            UserEntry user = userService.getByUsernameSecure(username);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found"));
            }

            UserSummaryDTO userSummary = UserSummaryDTO.builder()
                    .id(user.getId().toString())
                    .userName(user.getUserName())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .bio(user.getBio())
                    .location(user.getLocation())
                    .profileImageUrl(user.getProfileImageUrl())
                    .build();

            return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", userSummary));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve profile: " + e.getMessage()));
        }
    }

    /**
     * Update user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserSummaryDTO>> updateUserProfile(
            Authentication authentication,
            @RequestBody Map<String, Object> updates) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Authentication required"));
            }

            String username = authentication.getName();
            boolean updated = userService.updateUserPartial(username, updates);

            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found or update failed"));
            }

            // Return updated user profile
            UserEntry user = userService.getByUsernameSecure(username);
            UserSummaryDTO userSummary = UserSummaryDTO.builder()
                    .id(user.getId().toString())
                    .userName(user.getUserName())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .bio(user.getBio())
                    .location(user.getLocation())
                    .profileImageUrl(user.getProfileImageUrl())
                    .build();

            return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", userSummary));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update profile: " + e.getMessage()));
        }
    }

    /**
     * Change user password
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Object>> changePassword(
            Authentication authentication,
            @RequestBody Map<String, String> passwordData) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Authentication required"));
            }

            String username = authentication.getName();
            String currentPassword = passwordData.get("currentPassword");
            String newPassword = passwordData.get("newPassword");

            if (currentPassword == null || newPassword == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Current password and new password are required"));
            }

            if (newPassword.length() < 6) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("New password must be at least 6 characters long"));
            }

            boolean changed = userService.changeUserPassword(username, currentPassword, newPassword);

            if (!changed) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Current password is incorrect"));
            }

            return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to change password: " + e.getMessage()));
        }
    }

    /**
     * Delete user account
     */
    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<Object>> deleteUserAccount(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Authentication required"));
            }

            String username = authentication.getName();
            boolean deleted = userService.deleteUserByUsername(username);

            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found or deletion failed"));
            }

            return ResponseEntity.ok(ApiResponse.success("Account deleted successfully", null));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete account: " + e.getMessage()));
        }
    }
}
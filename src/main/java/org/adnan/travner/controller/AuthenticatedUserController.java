package org.adnan.travner.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.dto.UserDetailDTO;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.exception.ExceptionResponses;
import org.adnan.travner.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for authenticated user's own data
 * Handles /api/user/* endpoints (current logged-in user)
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class AuthenticatedUserController {

    private final UserService userService;

    /**
     * Get current authenticated user's profile
     * This endpoint is called by frontend after authentication
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDetailDTO>> getCurrentUserProfile(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            String username = authentication.getName();
            log.debug("Getting profile for authenticated user: {}", username);

            UserEntry user = userService.getByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found"));
            }

            return ResponseEntity.ok(ApiResponse.success(
                    "User profile retrieved successfully", UserDetailDTO.from(user)));

        } catch (Exception e) {
            return ExceptionResponses.from(e);
        }
    }

    /**
     * Delete current user account
     */
    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<Void>> deleteCurrentUserAccount(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            String username = authentication.getName();
            log.info("User {} is deleting their account", username);

            UserEntry user = userService.getByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found"));
            }

            boolean deleted = userService.deleteUser(user);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Failed to delete account"));
            }

            return ResponseEntity.ok(ApiResponse.success("Account deleted successfully", null));

        } catch (Exception e) {
            return ExceptionResponses.from(e);
        }
    }
}


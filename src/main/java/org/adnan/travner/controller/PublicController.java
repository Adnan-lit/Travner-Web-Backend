package org.adnan.travner.controller;

import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.dto.UserSummaryDTO;
import org.adnan.travner.exception.DuplicateUserException;
import org.adnan.travner.util.IdentityNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@Slf4j
public class PublicController {

    @Autowired
    private UserService userService;

    /**
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserSummaryDTO>> registerUser(@Valid @RequestBody UserEntry userRequest) {
        try {
            if (userRequest == null || userRequest.getUserName() == null || userRequest.getPassword() == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Username and password are required"));
            }

            // Canonical form is the only form ever stored or echoed back.
            userRequest.setUserName(IdentityNormalizer.normalizeUsername(userRequest.getUserName()));
            userRequest.setEmail(IdentityNormalizer.normalizeEmail(userRequest.getEmail()));

            // Check if username is available
            if (!userService.isUsernameAvailable(userRequest.getUserName())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error("Username or email already exists"));
            }

            // Create new user
            userService.saveNewUser(userRequest);

            // Return user info without password
            UserEntry createdUser = userService.getByUsernameSecure(userRequest.getUserName());
            UserSummaryDTO userSummary = UserSummaryDTO.builder()
                    .id(createdUser.getId().toString())
                    .userName(createdUser.getUserName())
                    .firstName(createdUser.getFirstName())
                    .lastName(createdUser.getLastName())
                    .email(createdUser.getEmail())
                    .bio(createdUser.getBio())
                    .location(createdUser.getLocation())
                    .roles(createdUser.getRoles())
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("User registered successfully with a lowercase username", userSummary));

        } catch (DuplicateUserException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Username or email already exists"));
        } catch (Exception e) {
            log.error("Registration failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Registration failed"));
        }
    }

    /**
     * Get public user information
     */
    @GetMapping("/user/{username}")
    public ResponseEntity<ApiResponse<UserSummaryDTO>> getPublicUserInfo(@PathVariable String username) {
        try {
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
                    .bio(user.getBio())
                    .location(user.getLocation())
                    .profileImageUrl(user.getProfileImageUrl())
                    .roles(user.getRoles())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(userSummary));

        } catch (Exception e) {
            log.error("Failed to retrieve public user info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve user info"));
        }
    }

    /**
     * Check username availability
     */
    @GetMapping("/check-username/{username}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkUsernameAvailability(@PathVariable String username) {
        try {
            boolean available = userService.isUsernameAvailable(username);
            return ResponseEntity.ok(ApiResponse.success("Username check completed",
                    Map.of("available", available)));
        } catch (Exception e) {
            log.error("Failed to check username availability", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to check username"));
        }
    }
}
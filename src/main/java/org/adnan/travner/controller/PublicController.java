package org.adnan.travner.controller;

import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.dto.UserSummaryDTO;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/public")
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

            // Check if username is available
            if (!userService.isUsernameAvailable(userRequest.getUserName())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error("Username already exists"));
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
                    .body(ApiResponse.success("User registered successfully", userSummary));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Registration failed: " + e.getMessage()));
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve user info: " + e.getMessage()));
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to check username: " + e.getMessage()));
        }
    }
}
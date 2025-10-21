package org.adnan.travner.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.dto.UserSummaryDTO;
import org.adnan.travner.dto.user.ProfileUpdateRequest;
import org.adnan.travner.dto.user.UserStatsDTO;
import org.adnan.travner.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;

/**
 * REST Controller for user management and search
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "User management and search APIs")
public class UserController {

    private final UserService userService;

    /**
     * Search users by name, username, or email
     */
    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Search users by name, username, or email")
    public ResponseEntity<ApiResponse<List<UserSummaryDTO>>> searchUsers(
            @Parameter(description = "Search query") @RequestParam String q,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        log.debug("Searching users with query: {} for user: {}", q, authentication.getName());

        Pageable pageable = PageRequest.of(page, size, Sort.by("userName").ascending());
        Page<UserSummaryDTO> users = userService.searchUsers(q, pageable);

        ApiResponse<List<UserSummaryDTO>> response = ApiResponse.<List<UserSummaryDTO>>builder()
                .success(true)
                .message("Users found successfully")
                .data(users.getContent())
                .pagination(ApiResponse.PaginationMeta.builder()
                        .page(users.getNumber())
                        .size(users.getSize())
                        .totalElements(users.getTotalElements())
                        .totalPages(users.getTotalPages())
                        .first(users.isFirst())
                        .last(users.isLast())
                        .build())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Get user details by user ID")
    public ResponseEntity<ApiResponse<UserSummaryDTO>> getUserById(
            @Parameter(description = "User ID") @PathVariable String userId,
            Authentication authentication) {

        log.debug("Getting user by ID: {} for user: {}", userId, authentication.getName());

        UserSummaryDTO user = userService.getUserById(userId);

        ApiResponse<UserSummaryDTO> response = ApiResponse.<UserSummaryDTO>builder()
                .success(true)
                .message("User found successfully")
                .data(user)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get user by username
     */
    @GetMapping("/username/{username}")
    @Operation(summary = "Get user by username", description = "Get user details by username")
    public ResponseEntity<ApiResponse<UserSummaryDTO>> getUserByUsername(
            @Parameter(description = "Username") @PathVariable String username,
            Authentication authentication) {

        log.debug("Getting user by username: {} for user: {}", username, authentication.getName());

        UserSummaryDTO user = userService.getUserByUsername(username);

        ApiResponse<UserSummaryDTO> response = ApiResponse.<UserSummaryDTO>builder()
                .success(true)
                .message("User found successfully")
                .data(user)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get user's followers
     */
    @GetMapping("/{userId}/followers")
    @Operation(summary = "Get user followers", description = "Get list of user's followers")
    public ResponseEntity<ApiResponse<List<UserSummaryDTO>>> getFollowers(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        log.debug("Getting followers for user: {} requested by: {}", userId, authentication.getName());

        Pageable pageable = PageRequest.of(page, size, Sort.by("userName").ascending());
        Page<UserSummaryDTO> followers = userService.getFollowers(userId, pageable);

        ApiResponse<List<UserSummaryDTO>> response = ApiResponse.<List<UserSummaryDTO>>builder()
                .success(true)
                .message("Followers retrieved successfully")
                .data(followers.getContent())
                .pagination(ApiResponse.PaginationMeta.builder()
                        .page(followers.getNumber())
                        .size(followers.getSize())
                        .totalElements(followers.getTotalElements())
                        .totalPages(followers.getTotalPages())
                        .first(followers.isFirst())
                        .last(followers.isLast())
                        .build())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get user's following
     */
    @GetMapping("/{userId}/following")
    @Operation(summary = "Get user following", description = "Get list of users that the user is following")
    public ResponseEntity<ApiResponse<List<UserSummaryDTO>>> getFollowing(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        log.debug("Getting following for user: {} requested by: {}", userId, authentication.getName());

        Pageable pageable = PageRequest.of(page, size, Sort.by("userName").ascending());
        Page<UserSummaryDTO> following = userService.getFollowing(userId, pageable);

        ApiResponse<List<UserSummaryDTO>> response = ApiResponse.<List<UserSummaryDTO>>builder()
                .success(true)
                .message("Following retrieved successfully")
                .data(following.getContent())
                .pagination(ApiResponse.PaginationMeta.builder()
                        .page(following.getNumber())
                        .size(following.getSize())
                        .totalElements(following.getTotalElements())
                        .totalPages(following.getTotalPages())
                        .first(following.isFirst())
                        .last(following.isLast())
                        .build())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Follow a user
     */
    @PostMapping("/{userId}/follow")
    @Operation(summary = "Follow user", description = "Follow another user")
    public ResponseEntity<ApiResponse<Void>> followUser(
            @Parameter(description = "User ID to follow") @PathVariable String userId,
            Authentication authentication) {

        log.debug("User {} following user {}", authentication.getName(), userId);

        userService.followUser(authentication.getName(), userId);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("User followed successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Unfollow a user
     */
    @DeleteMapping("/{userId}/unfollow")
    @Operation(summary = "Unfollow user", description = "Unfollow another user")
    public ResponseEntity<ApiResponse<Void>> unfollowUser(
            @Parameter(description = "User ID to unfollow") @PathVariable String userId,
            Authentication authentication) {

        log.debug("User {} unfollowing user {}", authentication.getName(), userId);

        userService.unfollowUser(authentication.getName(), userId);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("User unfollowed successfully")
                    .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Check if current user follows another user
     */
    @GetMapping("/{userId}/follow-status")
    @Operation(summary = "Check follow status", description = "Check if current user follows another user")
    public ResponseEntity<ApiResponse<Boolean>> isFollowing(
            @Parameter(description = "User ID to check") @PathVariable String userId,
            Authentication authentication) {

        log.debug("Checking if user {} follows user {}", authentication.getName(), userId);

        boolean following = userService.isFollowing(authentication.getName(), userId);

        ApiResponse<Boolean> response = ApiResponse.<Boolean>builder()
                .success(true)
                .message("Follow status retrieved successfully")
                .data(following)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get user statistics
     */
    @GetMapping("/{userId}/stats")
    @Operation(summary = "Get user statistics", description = "Get user's statistics including posts, followers, etc.")
    public ResponseEntity<ApiResponse<UserStatsDTO>> getUserStats(
            @Parameter(description = "User ID") @PathVariable String userId,
            Authentication authentication) {

        log.debug("Getting stats for user: {} requested by: {}", userId, authentication.getName());

        UserStatsDTO stats = userService.getUserStats(userId);

        ApiResponse<UserStatsDTO> response = ApiResponse.<UserStatsDTO>builder()
                .success(true)
                .message("User statistics retrieved successfully")
                .data(stats)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Update user profile
     */
    @PutMapping("/profile")
    @Operation(summary = "Update profile", description = "Update current user's profile")
    public ResponseEntity<ApiResponse<UserSummaryDTO>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            Authentication authentication) {

        log.debug("Updating profile for user: {}", authentication.getName());

        UserSummaryDTO user = userService.updateProfile(authentication.getName(), request);

        ApiResponse<UserSummaryDTO> response = ApiResponse.<UserSummaryDTO>builder()
                .success(true)
                .message("Profile updated successfully")
                .data(user)
                    .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Upload profile image
     */
    @PostMapping("/profile/image")
    @Operation(summary = "Upload profile image", description = "Upload profile image for current user")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadProfileImage(
            @RequestParam("image") MultipartFile image,
            Authentication authentication) {

        log.debug("Uploading profile image for user: {}", authentication.getName());

        ImageUploadResponse result = new ImageUploadResponse("placeholder-url"); // Placeholder

        ApiResponse<ImageUploadResponse> response = ApiResponse.<ImageUploadResponse>builder()
                .success(true)
                .message("Profile image uploaded successfully")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }

    // Inner DTO for image upload response
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageUploadResponse {
        private String imageUrl;
    }
}
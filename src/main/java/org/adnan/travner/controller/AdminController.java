package org.adnan.travner.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.dto.PostDTO;
import org.adnan.travner.dto.ProductDTO;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.service.CommentService;
import org.adnan.travner.service.PostService;
import org.adnan.travner.service.ProductService;
import org.adnan.travner.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin Controller for managing users, posts, products, and system statistics
 * All endpoints require ADMIN role
 */
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final PostService postService;
    private final ProductService productService;
    private final CommentService commentService;

    // ===========================================
    // USER MANAGEMENT
    // ===========================================

    /**
     * Get all users with pagination
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserEntry>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        try {
            // TODO: Implement pagination in UserService
            // For now, get all users (pagination can be added to UserService later)
            List<UserEntry> users = userService.getAll();

            // Log pagination parameters for future implementation
            log.info("Admin requested users with pagination: page={}, size={}, sortBy={}, direction={}",
                    page, size, sortBy, direction);

            return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
        } catch (Exception e) {
            log.error("Error retrieving users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve users: " + e.getMessage()));
        }
    }

    /**
     * Get user by username
     */
    @GetMapping("/users/{username}")
    public ResponseEntity<ApiResponse<UserEntry>> getUserByUsername(@PathVariable String username) {
        try {
            UserEntry user = userService.getByUsernameSecure(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found: " + username));
            }
            return ResponseEntity.ok(ApiResponse.success(user));
        } catch (Exception e) {
            log.error("Error retrieving user {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve user: " + e.getMessage()));
        }
    }

    /**
     * Create new admin user
     */
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserEntry>> createAdminUser(@Valid @RequestBody UserEntry userEntry) {
        try {
            // Check if user already exists
            if (!userService.isUsernameAvailable(userEntry.getUserName())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error("Username already exists"));
            }

            // Set admin roles
            userEntry.setRoles(List.of("USER", "ADMIN"));
            userService.saveNewUser(userEntry);

            // Return user without password
            UserEntry createdUser = userService.getByUsernameSecure(userEntry.getUserName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Admin user created successfully", createdUser));
        } catch (Exception e) {
            log.error("Error creating admin user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to create admin user: " + e.getMessage()));
        }
    }

    /**
     * Update user roles
     */
    @PutMapping("/users/{username}/roles")
    public ResponseEntity<ApiResponse<UserEntry>> updateUserRoles(
            @PathVariable String username,
            @RequestBody Map<String, List<String>> request) {
        try {
            List<String> roles = request.get("roles");
            if (roles == null || roles.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Roles are required"));
            }

            boolean updated = userService.updateUserRoles(username, roles);
            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found"));
            }

            UserEntry updatedUser = userService.getByUsernameSecure(username);
            return ResponseEntity.ok(ApiResponse.success("User roles updated successfully", updatedUser));
        } catch (Exception e) {
            log.error("Error updating user roles for {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update user roles: " + e.getMessage()));
        }
    }

    /**
     * Set user active/inactive status
     */
    @PutMapping("/users/{username}/status")
    public ResponseEntity<ApiResponse<UserEntry>> setUserActiveStatus(
            @PathVariable String username,
            @RequestBody Map<String, Boolean> request) {
        try {
            Boolean active = request.get("active");
            if (active == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Active status is required"));
            }

            boolean updated = userService.setUserActiveStatus(username, active);
            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found"));
            }

            UserEntry updatedUser = userService.getByUsernameSecure(username);
            String message = active ? "User activated successfully" : "User deactivated successfully";
            return ResponseEntity.ok(ApiResponse.success(message, updatedUser));
        } catch (Exception e) {
            log.error("Error updating user status for {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update user status: " + e.getMessage()));
        }
    }

    /**
     * Delete user
     */
    @DeleteMapping("/users/{username}")
    public ResponseEntity<ApiResponse<Object>> deleteUser(
            Authentication authentication,
            @PathVariable String username) {
        try {
            // Prevent admin from deleting themselves
            if (authentication != null && username.equals(authentication.getName())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Cannot delete your own account"));
            }

            boolean deleted = userService.deleteUserByUsername(username);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found"));
            }

            return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting user {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete user: " + e.getMessage()));
        }
    }

    /**
     * Reset user password
     */
    @PutMapping("/users/{username}/password")
    public ResponseEntity<ApiResponse<Object>> resetUserPassword(
            @PathVariable String username,
            @RequestBody Map<String, String> request) {
        try {
            String newPassword = request.get("password");
            if (newPassword == null || newPassword.length() < 6) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Password must be at least 6 characters long"));
            }

            boolean updated = userService.resetUserPassword(username, newPassword);
            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found"));
            }

            return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
        } catch (Exception e) {
            log.error("Error resetting password for {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to reset password: " + e.getMessage()));
        }
    }

    // ===========================================
    // CONTENT MANAGEMENT
    // ===========================================

    /**
     * Get all posts (including unpublished) with pagination
     */
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<List<PostDTO>>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        try {
            Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

            // Get all posts including unpublished ones (admin can see all)
            Page<PostDTO> posts = postService.getAllPosts(pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(posts));
        } catch (Exception e) {
            log.error("Error retrieving posts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve posts: " + e.getMessage()));
        }
    }

    /**
     * Delete any post (admin privilege)
     */
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Object>> deletePost(@PathVariable String id) {
        try {
            // Admin can delete any post - the PostService now checks for admin role
            postService.deletePost(id, "admin");
            return ResponseEntity.ok(ApiResponse.success("Post deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting post {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete post: " + e.getMessage()));
        }
    }

    /**
     * Get all products with pagination
     */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        try {
            Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

            Page<ProductDTO> products = productService.getAllAvailableProducts(pageable);
            return ResponseEntity.ok(ApiResponse.fromPage(products));
        } catch (Exception e) {
            log.error("Error retrieving products: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve products: " + e.getMessage()));
        }
    }

    /**
     * Delete any product (admin privilege)
     */
    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteProduct(@PathVariable String id) {
        try {
            productService.deleteProduct(id, "admin"); // Now supports admin deletion
            return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting product {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete product: " + e.getMessage()));
        }
    }

    // ===========================================
    // STATISTICS & ANALYTICS
    // ===========================================

    /**
     * Get comprehensive system statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // User statistics
            long totalUsers = userService.getUserCount();
            long adminUsers = userService.getUsersByRole("ADMIN").size();
            long regularUsers = userService.getUsersByRole("USER").size();

            stats.put("users", Map.of(
                "total", totalUsers,
                "admins", adminUsers,
                "regular", regularUsers,
                "activeUsers", totalUsers // TODO: Add active user count when user activity tracking is implemented
            ));

            // Content statistics - now using actual count methods
            stats.put("content", Map.of(
                "totalPosts", postService.getPostCount(),
                "totalProducts", productService.getProductCount(),
                "totalComments", commentService.getCommentCount(),
                "availableProducts", productService.getAvailableProductCount()
            ));

            stats.put("timestamp", System.currentTimeMillis());
            stats.put("serverTime", java.time.LocalDateTime.now());

            return ResponseEntity.ok(ApiResponse.success("Statistics retrieved successfully", stats));
        } catch (Exception e) {
            log.error("Error retrieving statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve statistics: " + e.getMessage()));
        }
    }

    /**
     * Get users by role
     */
    @GetMapping("/users/role/{role}")
    public ResponseEntity<ApiResponse<List<UserEntry>>> getUsersByRole(@PathVariable String role) {
        try {
            List<UserEntry> users = userService.getUsersByRole(role.toUpperCase());
            return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
        } catch (Exception e) {
            log.error("Error retrieving users by role {}: {}", role, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve users by role: " + e.getMessage()));
        }
    }

    /**
     * Promote user to admin
     */
    @PostMapping("/users/{username}/promote")
    public ResponseEntity<ApiResponse<UserEntry>> promoteUserToAdmin(@PathVariable String username) {
        try {
            boolean promoted = userService.promoteUserToAdmin(username);
            if (!promoted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found"));
            }

            UserEntry updatedUser = userService.getByUsernameSecure(username);
            return ResponseEntity.ok(ApiResponse.success("User promoted to admin successfully", updatedUser));
        } catch (Exception e) {
            log.error("Error promoting user {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to promote user: " + e.getMessage()));
        }
    }
}

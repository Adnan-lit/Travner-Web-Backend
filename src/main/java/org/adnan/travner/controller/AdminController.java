package org.adnan.travner.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.dto.PostDTO;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.PostRepository;
import org.adnan.travner.repository.UserRepository;
import org.adnan.travner.service.UserService;
import org.adnan.travner.service.PostService;
import org.adnan.travner.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin controller for maintenance operations and user management
 */
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final UserService userService;
    private final PostService postService;
    private final ProductService productService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Check if user has admin role
     */
    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }

    // ===========================================
    // USER MANAGEMENT
    // ===========================================

    /**
     * Get all users with pagination
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Admin access required"));
        }

        try {
            // Create sort object
            Sort sort = direction.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();

            // Create pageable
            Pageable pageable = PageRequest.of(page, size, sort);

            // Build query
            Query query = new Query();
            
            // Add search filter
            if (search != null && !search.trim().isEmpty()) {
                query.addCriteria(new Criteria().orOperator(
                    Criteria.where("userName").regex(search, "i"),
                    Criteria.where("firstName").regex(search, "i"),
                    Criteria.where("lastName").regex(search, "i"),
                    Criteria.where("email").regex(search, "i")
                ));
            }

            // Add role filter
            if (role != null && !role.trim().isEmpty()) {
                query.addCriteria(Criteria.where("roles").in(role));
            }

            // Get total count
            long totalElements = mongoTemplate.count(query, UserEntry.class);

            // Get paginated results
            query.with(pageable);
            List<UserEntry> users = mongoTemplate.find(query, UserEntry.class);

            // Remove passwords for security
            users.forEach(user -> user.setPassword(""));

            // Create response with pagination info
            Map<String, Object> response = new HashMap<>();
            response.put("content", users);
            response.put("totalElements", totalElements);
            response.put("totalPages", (int) Math.ceil((double) totalElements / size));
            response.put("currentPage", page);
            response.put("size", size);
            response.put("first", page == 0);
            response.put("last", page >= (int) Math.ceil((double) totalElements / size) - 1);

            return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", response));

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
    public ResponseEntity<ApiResponse<UserEntry>> getUserByUsername(
            @PathVariable String username,
            Authentication authentication) {
        
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Admin access required"));
        }

        try {
            UserEntry user = userService.getByUsernameSecure(username);
            if (user != null) {
                user.setPassword(""); // Remove password for security
                return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found"));
            }
        } catch (Exception e) {
            log.error("Error retrieving user {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve user: " + e.getMessage()));
        }
    }

    /**
     * Update user roles
     */
    @PutMapping("/users/{username}/roles")
    public ResponseEntity<ApiResponse<UserEntry>> updateUserRoles(
            @PathVariable String username,
            @RequestBody Map<String, List<String>> roleRequest,
            Authentication authentication) {
        
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Admin access required"));
        }

        try {
            List<String> roles = roleRequest.get("roles");
            if (roles == null || roles.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Roles are required"));
            }

            UserEntry user = userRepository.findByuserName(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found"));
            }

            user.setRoles(roles);
            userRepository.save(user);
            user.setPassword(""); // Remove password for security

            log.info("Admin {} updated roles for user {} to {}", 
                authentication.getName(), username, roles);

            return ResponseEntity.ok(ApiResponse.success("User roles updated successfully", user));

        } catch (Exception e) {
            log.error("Error updating roles for user {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update user roles: " + e.getMessage()));
        }
    }

    /**
     * Activate user
     */
    @PutMapping("/users/{username}/activate")
    public ResponseEntity<ApiResponse<UserEntry>> activateUser(
            @PathVariable String username,
            Authentication authentication) {
        
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Admin access required"));
        }

        try {
            UserEntry user = userRepository.findByuserName(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found"));
            }

            user.setActive(true);
            userRepository.save(user);
            user.setPassword(""); // Remove password for security

            log.info("Admin {} activated user {}", authentication.getName(), username);

            return ResponseEntity.ok(ApiResponse.success("User activated successfully", user));

        } catch (Exception e) {
            log.error("Error activating user {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to activate user: " + e.getMessage()));
        }
    }

    /**
     * Deactivate user
     */
    @PutMapping("/users/{username}/deactivate")
    public ResponseEntity<ApiResponse<UserEntry>> deactivateUser(
            @PathVariable String username,
            Authentication authentication) {
        
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Admin access required"));
        }

        try {
            UserEntry user = userRepository.findByuserName(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found"));
            }

            user.setActive(false);
            userRepository.save(user);
            user.setPassword(""); // Remove password for security

            log.info("Admin {} deactivated user {}", authentication.getName(), username);

            return ResponseEntity.ok(ApiResponse.success("User deactivated successfully", user));

        } catch (Exception e) {
            log.error("Error deactivating user {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to deactivate user: " + e.getMessage()));
        }
    }

    /**
     * Delete user
     */
    @DeleteMapping("/users/{username}")
    public ResponseEntity<ApiResponse<String>> deleteUser(
            @PathVariable String username,
            Authentication authentication) {
        
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Admin access required"));
        }

        try {
            UserEntry user = userRepository.findByuserName(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found"));
            }

            // Don't allow deleting admin users
            if (user.getRoles() != null && user.getRoles().contains("ADMIN")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Cannot delete admin users"));
            }

            userRepository.delete(user);

            log.info("Admin {} deleted user {}", authentication.getName(), username);

            return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));

        } catch (Exception e) {
            log.error("Error deleting user {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete user: " + e.getMessage()));
        }
    }

    // ===========================================
    // CONTENT MANAGEMENT
    // ===========================================

    /**
     * Get all posts with admin details
     */
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Admin access required"));
        }

        try {
            // Use PostService to get posts with pagination
            Pageable pageable = PageRequest.of(page, size, 
                Sort.by(direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));
            
            Page<PostDTO> posts = postService.getAllPosts(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", posts.getContent());
            response.put("totalElements", posts.getTotalElements());
            response.put("totalPages", posts.getTotalPages());
            response.put("currentPage", posts.getNumber());
            response.put("size", posts.getSize());
            response.put("first", posts.isFirst());
            response.put("last", posts.isLast());

            return ResponseEntity.ok(ApiResponse.success("Posts retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error retrieving posts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve posts: " + e.getMessage()));
        }
    }

    /**
     * Delete any post (admin override)
     */
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<String>> deletePost(
            @PathVariable String postId,
            Authentication authentication) {
        
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Admin access required"));
        }

        try {
            postService.deletePost(postId, authentication.getName());

            log.info("Admin {} deleted post {}", authentication.getName(), postId);

            return ResponseEntity.ok(ApiResponse.success("Post deleted successfully", null));

        } catch (Exception e) {
            log.error("Error deleting post {}: {}", postId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete post: " + e.getMessage()));
        }
    }

    // ===========================================
    // SYSTEM STATISTICS
    // ===========================================

    /**
     * Get statistics about posts and media
     */
    @GetMapping("/stats/posts")
    public ResponseEntity<ApiResponse<Object>> getPostStats(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Admin access required"));
        }

        try {
            long totalPosts = postRepository.count();
            long postsWithMedia = mongoTemplate.count(
                new Query(Criteria.where("mediaUrls").exists(true).ne(Arrays.asList())), 
                "posts"
            );
            long postsWithInvalidMedia = mongoTemplate.count(
                new Query(Criteria.where("mediaUrls").in(Arrays.asList("/api/media/null", "null"))), 
                "posts"
            );

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalPosts", totalPosts);
            stats.put("postsWithMedia", postsWithMedia);
            stats.put("postsWithInvalidMedia", postsWithInvalidMedia);
            
            return ResponseEntity.ok(ApiResponse.success("Post statistics retrieved", stats));

        } catch (Exception e) {
            log.error("Error getting post stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get post statistics: " + e.getMessage()));
        }
    }

    /**
     * Get system statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStats(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Admin access required"));
        }

        try {
            long totalUsers = userRepository.count();
            long adminUsers = mongoTemplate.count(
                new Query(Criteria.where("roles").in("ADMIN")), 
                UserEntry.class
            );
            long activeUsers = mongoTemplate.count(
                new Query(Criteria.where("active").is(true)), 
                UserEntry.class
            );
            long totalPosts = postRepository.count();
            
            // Get product count if ProductService is available
            long totalProducts = 0;
            try {
                totalProducts = productService.getProductCount();
            } catch (Exception e) {
                log.warn("Could not get product count: {}", e.getMessage());
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", totalUsers);
            stats.put("adminUsers", adminUsers);
            stats.put("activeUsers", activeUsers);
            stats.put("totalPosts", totalPosts);
            stats.put("totalProducts", totalProducts);
            
            return ResponseEntity.ok(ApiResponse.success("System statistics retrieved", stats));

        } catch (Exception e) {
            log.error("Error getting system stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get system statistics: " + e.getMessage()));
        }
    }

    // ===========================================
    // MAINTENANCE OPERATIONS
    // ===========================================

    /**
     * Clean up invalid media URLs from posts
     */
    @PostMapping("/cleanup/media-urls")
    public ResponseEntity<ApiResponse<String>> cleanupInvalidMediaUrls(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Admin access required"));
        }

        try {
            // Find posts with invalid media URLs
            Query query = new Query(Criteria.where("mediaUrls").in(Arrays.asList("/api/media/null", "null")));
            long countBefore = mongoTemplate.count(query, "posts");
            
            log.info("Found {} posts with invalid media URLs", countBefore);

            // Remove invalid media URLs
            Update update = new Update().pull("mediaUrls", new org.springframework.data.mongodb.core.query.Criteria().in(Arrays.asList("/api/media/null", "null")));
            mongoTemplate.updateMulti(query, update, "posts");

            // Set empty mediaUrls for posts that only had invalid URLs
            Query emptyQuery = new Query(Criteria.where("mediaUrls").size(0));
            mongoTemplate.updateMulti(emptyQuery, new Update().set("mediaUrls", Arrays.asList()), "posts");

            // Verify cleanup
            long countAfter = mongoTemplate.count(query, "posts");
            
            log.info("Cleanup completed. {} posts still have invalid media URLs", countAfter);

            return ResponseEntity.ok(ApiResponse.success(
                String.format("Cleanup completed. Removed invalid media URLs from %d posts", countBefore - countAfter), 
                null
            ));

        } catch (Exception e) {
            log.error("Error during media URL cleanup: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to cleanup media URLs: " + e.getMessage()));
        }
    }
}
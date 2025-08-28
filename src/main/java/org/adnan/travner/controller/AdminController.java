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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    /**
     * Get all users (Admin only)
     * GET /admin/users
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserEntry>> getAllUsers() {
        try {
            List<UserEntry> users = userService.getAll();
            return new ResponseEntity<>(users, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get user by username (Admin only)
     * GET /admin/users/{username}
     */
    @GetMapping("/users/{username}")
    public ResponseEntity<UserEntry> getUserByUsername(@PathVariable String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            UserEntry user = userService.getByUsernameSecure(username);
            if (user == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // Password already removed in UserService.getByUsernameSecure()
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete user by username (Admin only)
     * DELETE /admin/users/{username}
     */
    @DeleteMapping("/users/{username}")
    public ResponseEntity<Map<String, String>> deleteUserByUsername(@PathVariable String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return new ResponseEntity<>(createResponse("error", "Username is required"), HttpStatus.BAD_REQUEST);
            }

            // Prevent admin from deleting themselves
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && username.equals(auth.getName())) {
                return new ResponseEntity<>(createResponse("error", "Cannot delete your own account"),
                        HttpStatus.FORBIDDEN);
            }

            boolean deleted = userService.deleteUserByUsername(username);
            if (!deleted) {
                return new ResponseEntity<>(createResponse("error", "User not found or could not be deleted"),
                        HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(createResponse("message", "User deleted successfully"), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(createResponse("error", "Internal server error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update user roles (Admin only)
     * PUT /admin/users/{username}/roles
     * Body: {"roles": ["USER", "ADMIN"]}
     */
    @PutMapping("/users/{username}/roles")
    public ResponseEntity<Map<String, String>> updateUserRoles(
            @PathVariable String username,
            @RequestBody Map<String, List<String>> request) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return new ResponseEntity<>(createResponse("error", "Username is required"), HttpStatus.BAD_REQUEST);
            }

            List<String> roles = request.get("roles");
            if (roles == null || roles.isEmpty()) {
                return new ResponseEntity<>(createResponse("error", "Roles are required"), HttpStatus.BAD_REQUEST);
            }

            // Validate roles (only allow USER and ADMIN for now)
            for (String role : roles) {
                if (!role.equals("USER") && !role.equals("ADMIN")) {
                    return new ResponseEntity<>(createResponse("error", "Invalid role: " + role),
                            HttpStatus.BAD_REQUEST);
                }
            }

            boolean updated = userService.updateUserRoles(username, roles);
            if (!updated) {
                return new ResponseEntity<>(createResponse("error", "User not found or could not be updated"),
                        HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(createResponse("message", "User roles updated successfully"), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(createResponse("error", "Internal server error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Reset user password (Admin only)
     * PUT /admin/users/{username}/password
     * Body: {"password": "new-password"}
     */
    @PutMapping("/users/{username}/password")
    public ResponseEntity<Map<String, String>> resetUserPassword(
            @PathVariable String username,
            @RequestBody Map<String, String> request) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return new ResponseEntity<>(createResponse("error", "Username is required"), HttpStatus.BAD_REQUEST);
            }

            String newPassword = request.get("password");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return new ResponseEntity<>(createResponse("error", "Password is required"), HttpStatus.BAD_REQUEST);
            }

            if (newPassword.length() < 6) {
                return new ResponseEntity<>(createResponse("error", "Password must be at least 6 characters long"),
                        HttpStatus.BAD_REQUEST);
            }

            boolean updated = userService.resetUserPassword(username, newPassword);
            if (!updated) {
                return new ResponseEntity<>(createResponse("error", "User not found or could not be updated"),
                        HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(createResponse("message", "Password reset successfully"), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(createResponse("error", "Internal server error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Promote user to admin (Admin only)
     * POST /admin/users/{username}/promote
     */
    @PostMapping("/users/{username}/promote")
    public ResponseEntity<Map<String, String>> promoteUserToAdmin(@PathVariable String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return new ResponseEntity<>(createResponse("error", "Username is required"), HttpStatus.BAD_REQUEST);
            }

            boolean promoted = userService.promoteUserToAdmin(username);
            if (!promoted) {
                return new ResponseEntity<>(createResponse("error", "User not found or could not be promoted"),
                        HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(createResponse("message", "User promoted to admin successfully"),
                    HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(createResponse("error", "Internal server error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get users by role (Admin only)
     * GET /admin/users/role/{role}
     */
    @GetMapping("/users/role/{role}")
    public ResponseEntity<List<UserEntry>> getUsersByRole(@PathVariable String role) {
        try {
            if (role == null || role.trim().isEmpty()) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            List<UserEntry> users = userService.getUsersByRole(role.toUpperCase());
            // Passwords already removed in UserService.getUsersByRole()
            return new ResponseEntity<>(users, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get system statistics (Admin only)
     * GET /admin/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            long totalUsers = userService.getUserCount();
            long adminUsers = userService.getUsersByRole("ADMIN").size();
            long regularUsers = userService.getUsersByRole("USER").size();

            stats.put("totalUsers", totalUsers);
            stats.put("adminUsers", adminUsers);
            stats.put("regularUsers", regularUsers);
            stats.put("timestamp", System.currentTimeMillis());

            return new ResponseEntity<>(stats, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Create admin user (Admin only)
     * POST /admin/users
     * Body: {"userName": "admin", "password": "password", "firstName": "Admin",
     * "lastName": "User", "email": "admin@example.com"}
     */
    @PostMapping("/users")
    public ResponseEntity<Map<String, String>> createAdminUser(@RequestBody UserEntry userEntry) {
        try {
            if (userEntry.getUserName().trim().isEmpty()) {
                return new ResponseEntity<>(createResponse("error", "Username is required"), HttpStatus.BAD_REQUEST);
            }

            if (userEntry.getPassword().trim().isEmpty()) {
                return new ResponseEntity<>(createResponse("error", "Password is required"), HttpStatus.BAD_REQUEST);
            }

            if (userEntry.getPassword().length() < 6) {
                return new ResponseEntity<>(createResponse("error", "Password must be at least 6 characters long"),
                        HttpStatus.BAD_REQUEST);
            }

            // Check if user already exists
            UserEntry existingUser = userService.getByUsername(userEntry.getUserName());
            if (existingUser != null) {
                return new ResponseEntity<>(createResponse("error", "User already exists"), HttpStatus.CONFLICT);
            }

            // Set admin roles
            userEntry.setRoles(List.of("USER", "ADMIN"));
            userService.saveNewUser(userEntry);

            return new ResponseEntity<>(createResponse("message", "Admin user created successfully"),
                    HttpStatus.CREATED);
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
}
package org.adnan.travner.service;

import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.bson.types.ObjectId;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    public List<UserEntry> getAll() {
        List<UserEntry> users = userRepository.findAll();
        // Remove passwords for security
        users.forEach(user -> user.setPassword(""));
        return users;
    }

    @Transactional
    public void saveNewUser(UserEntry user) {
        try {
            // Validate user data
            if (user == null || user.getUserName() == null || user.getPassword() == null) {
                throw new IllegalArgumentException("User data is incomplete");
            }
            
            // Check if username already exists
            if (!isUsernameAvailable(user.getUserName())) {
                throw new IllegalArgumentException("Username already exists");
            }
            
            // Check if email already exists
            if (user.getEmail() != null && getUserByEmail(user.getEmail()) != null) {
                throw new IllegalArgumentException("Email already exists");
            }
            
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setRoles(List.of("USER"));
            user.setCreatedAt(java.time.LocalDateTime.now());
            user.setActive(true);
            userRepository.save(user);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save user: " + e.getMessage(), e);
        }
    }

    public void saveUser(UserEntry user) {
        // Check if password needs encoding (not already encoded)
        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userRepository.save(user);
    }

    public UserEntry getByUsername(String username) {
        return userRepository.findByuserName(username);
    }

    public UserEntry getByUsernameSecure(String username) {
        UserEntry user = userRepository.findByuserName(username);
        if (user != null) {
            user.setPassword(""); // Remove password for security
        }
        return user;
    }

    @Transactional
    public boolean deleteUser(UserEntry user) {
        if (user == null) {
            return false;
        }
        try {
            userRepository.delete(user);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    // Admin-specific methods
    public boolean deleteUserById(String userId) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return false;
            }
            ObjectId objectId = new ObjectId(userId);
            userRepository.deleteById(objectId);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public boolean deleteUserByUsername(String username) {
        try {
            UserEntry user = getByUsername(username);
            if (user == null) {
                return false;
            }
            userRepository.delete(user);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public boolean updateUserRoles(String username, List<String> roles) {
        try {
            UserEntry user = getByUsername(username);
            if (user == null) {
                return false;
            }
            user.setRoles(roles);
            userRepository.save(user);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public boolean resetUserPassword(String username, String newPassword) {
        try {
            UserEntry user = getByUsername(username);
            if (user == null) {
                return false;
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public long getUserCount() {
        return userRepository.count();
    }

    public List<UserEntry> getUsersByRole(String role) {
        List<UserEntry> users = userRepository.findAll().stream()
                .filter(user -> user.getRoles() != null && user.getRoles().contains(role))
                .toList();
        // Remove passwords for security
        users.forEach(user -> user.setPassword(""));
        return users;
    }

    public boolean promoteUserToAdmin(String username) {
        try {
            UserEntry user = getByUsername(username);
            if (user == null) {
                return false;
            }
            List<String> roles = user.getRoles();
            if (roles == null) {
                roles = List.of("USER", "ADMIN");
            } else if (!roles.contains("ADMIN")) {
                java.util.List<String> mutableRoles = new java.util.ArrayList<>(roles);
                mutableRoles.add("ADMIN");
                roles = mutableRoles;
            }
            user.setRoles(roles);
            userRepository.save(user);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    // User profile management methods
    public boolean updateUserProfile(String username, String firstName, String lastName, String email) {
        try {
            UserEntry user = getByUsername(username);
            if (user == null) {
                return false;
            }
            if (firstName != null)
                user.setFirstName(firstName);
            if (lastName != null)
                user.setLastName(lastName);
            if (email != null)
                user.setEmail(email);
            userRepository.save(user);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    // Enhanced profile update method with additional fields
    public boolean updateUserProfileEnhanced(String username, String firstName, String lastName,
            String email, String bio, String location) {
        try {
            UserEntry user = getByUsername(username);
            if (user == null) {
                return false;
            }
            if (firstName != null)
                user.setFirstName(firstName);
            if (lastName != null)
                user.setLastName(lastName);
            if (email != null)
                user.setEmail(email);
            if (bio != null)
                user.setBio(bio);
            if (location != null)
                user.setLocation(location);
            userRepository.save(user);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Transactional
    public boolean changeUserPassword(String username, String currentPassword, String newPassword) {
        try {
            if (username == null || currentPassword == null || newPassword == null) {
                throw new IllegalArgumentException("All password fields are required");
            }
            
            if (newPassword.length() < 6) {
                throw new IllegalArgumentException("New password must be at least 6 characters long");
            }
            
            UserEntry user = getByUsername(username);
            if (user == null) {
                throw new IllegalArgumentException("User not found");
            }
            
            // Verify current password
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new IllegalArgumentException("Current password is incorrect");
            }
            
            // Update with new password
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to change password: " + e.getMessage(), e);
        }
    }

    public boolean isUsernameAvailable(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return getByUsername(username.trim()) == null;
    }

    public boolean updateUserPartial(String username, java.util.Map<String, Object> updates) {
        try {
            UserEntry user = getByUsername(username);
            if (user == null) {
                return false;
            }

            // Apply partial updates
            updates.forEach((key, value) -> {
                switch (key) {
                    case "firstName" -> {
                        if (value instanceof String)
                            user.setFirstName((String) value);
                    }
                    case "lastName" -> {
                        if (value instanceof String)
                            user.setLastName((String) value);
                    }
                    case "email" -> {
                        if (value instanceof String)
                            user.setEmail((String) value);
                    }
                    case "bio" -> {
                        if (value instanceof String)
                            user.setBio((String) value);
                    }
                    case "location" -> {
                        if (value instanceof String)
                            user.setLocation((String) value);
                    }
                    case "profileImageUrl" -> {
                        if (value instanceof String)
                            user.setProfileImageUrl((String) value);
                    }
                    // Note: username and password changes should use dedicated methods
                }
            });

            userRepository.save(user);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    // Password reset flow methods (simplified without Redis)
    public String generatePasswordResetToken(String username) {
        try {
            UserEntry user = getByUsername(username);
            if (user == null) {
                return null; // Don't reveal if user exists or not
            }

            // For now, return a simple token (in production, use proper token storage)
            String token = UUID.randomUUID().toString();
            
            // In production, you would send this token via email
            // For now, we'll return it (which is not secure but allows testing)
            return token;
        } catch (RuntimeException e) {
            return null;
        }
    }

    public boolean resetPasswordWithToken(String token, String newPassword) {
        try {
            if (token == null || newPassword == null || newPassword.length() < 6) {
                return false;
            }

            // For now, accept any token (in production, validate against stored tokens)
            // This is a simplified implementation for development
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    // Method to get user by email (for password reset)
    public UserEntry getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return userRepository.findAll().stream()
                .filter(user -> email.trim().equalsIgnoreCase(user.getEmail()))
                .findFirst()
                .orElse(null);
    }

    // Update last login time
    public void updateLastLogin(String username) {
        try {
            UserEntry user = getByUsername(username);
            if (user != null) {
                user.setLastLoginAt(java.time.LocalDateTime.now());
                userRepository.save(user);
            }
        } catch (RuntimeException e) {
            // Log error but don't fail authentication
        }
    }

    // Set user active/inactive status
    public boolean setUserActiveStatus(String username, boolean active) {
        try {
            UserEntry user = getByUsername(username);
            if (user == null) {
                return false;
            }
            user.setActive(active);
            userRepository.save(user);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    // Add getById method to fix compilation error
    public UserEntry getById(String id) {
        try {
            if (id == null || id.trim().isEmpty()) {
                return null;
            }
            ObjectId objectId = new ObjectId(id);
            UserEntry user = userRepository.findById(objectId).orElse(null);
            if (user != null) {
                user.setPassword(""); // Remove password for security
            }
            return user;
        } catch (RuntimeException e) {
            return null;
        }
    }
}

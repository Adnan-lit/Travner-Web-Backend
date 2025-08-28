package org.adnan.travner.service;

import lombok.Getter;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import org.bson.types.ObjectId;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Simple in-memory storage for password reset tokens
    // In production, this should be stored in Redis or database
    private final ConcurrentHashMap<String, PasswordResetToken> resetTokens = new ConcurrentHashMap<>();

    // Inner class for password reset token
    private static class PasswordResetToken {
        @Getter
        private final String username;
        private final LocalDateTime expiry;

        public PasswordResetToken(String username) {
            this.username = username;
            this.expiry = LocalDateTime.now().plusMinutes(15); // 15 minutes expiry
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiry);
        }
    }

    public List<UserEntry> getAll() {
        List<UserEntry> users = userRepository.findAll();
        // Remove passwords for security
        users.forEach(user -> user.setPassword(""));
        return users;
    }

    public void saveNewUser(UserEntry user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(List.of("USER"));
        userRepository.save(user);
    }

    public void saveUser(UserEntry user) {
        // Check if password needs encoding (not already encoded)
        if (!user.getPassword().startsWith("$2a$")) {
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

    public boolean changeUserPassword(String username, String currentPassword, String newPassword) {
        try {
            UserEntry user = getByUsername(username);
            if (user == null) {
                return false;
            }
            // Verify current password
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                return false;
            }
            // Update with new password
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return true;
        } catch (RuntimeException e) {
            return false;
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
                    // Note: username and password changes should use dedicated methods
                }
            });

            userRepository.save(user);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    // Password reset flow methods
    public String generatePasswordResetToken(String username) {
        try {
            UserEntry user = getByUsername(username);
            if (user == null) {
                return null; // Don't reveal if user exists or not
            }

            String token = UUID.randomUUID().toString();
            resetTokens.put(token, new PasswordResetToken(username));

            // Clean up expired tokens
            cleanupExpiredTokens();

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

            PasswordResetToken resetToken = resetTokens.get(token);
            if (resetToken == null || resetToken.isExpired()) {
                resetTokens.remove(token); // Remove expired token
                return false;
            }

            UserEntry user = getByUsername(resetToken.getUsername());
            if (user == null) {
                resetTokens.remove(token);
                return false;
            }

            // Update password
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            // Remove used token
            resetTokens.remove(token);

            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void cleanupExpiredTokens() {
        resetTokens.entrySet().removeIf(entry -> entry.getValue().isExpired());
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
}

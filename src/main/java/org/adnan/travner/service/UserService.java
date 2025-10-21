package org.adnan.travner.service;

import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.user.ProfileUpdateRequest;
import org.adnan.travner.dto.user.UserStatsDTO;
import org.adnan.travner.entry.FollowEntry;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.CommentRepository;
import org.adnan.travner.repository.FollowRepository;
import org.adnan.travner.repository.PostRepository;
import org.adnan.travner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.adnan.travner.dto.UserSummaryDTO;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private FollowRepository followRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private CommentRepository commentRepository;


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
            return deleteUser(user);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public boolean isUsernameAvailable(String username) {
        return getByUsername(username) == null;
    }

    public UserEntry getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void updateLastLogin(String username) {
        UserEntry user = getByUsername(username);
        if (user != null) {
            user.setLastLoginAt(java.time.LocalDateTime.now());
            userRepository.save(user);
        }
    }

    public Page<UserEntry> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public List<UserEntry> searchUsers(String query) {
        return userRepository.searchUsers(query, Pageable.unpaged()).getContent();
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

    // Add getById method that accepts ObjectId directly
    public UserEntry getById(ObjectId id) {
        try {
            if (id == null) {
                return null;
            }
            UserEntry user = userRepository.findById(id).orElse(null);
            if (user != null) {
                user.setPassword(""); // Remove password for security
            }
            return user;
        } catch (Exception e) {
            log.error("Error finding user by ObjectId: {}", id, e);
            return null;
        }
    }

    /**
     * Search users by username, firstName, or lastName
     */
    public Page<UserSummaryDTO> searchUsers(String query, Pageable pageable) {
        try {
            Page<UserEntry> users = userRepository.searchUsers(query, pageable);
            return users.map(this::convertToUserSummaryDTO);
        } catch (RuntimeException e) {
            return Page.empty(pageable);
        }
    }

    /**
     * Get user by ID
     */
    public UserSummaryDTO getUserById(String userId) {
        try {
            UserEntry user = userRepository.findById(new ObjectId(userId)).orElse(null);
            return user != null ? convertToUserSummaryDTO(user) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get user by username
     */
    public UserSummaryDTO getUserByUsername(String username) {
        try {
            UserEntry user = userRepository.findByuserName(username);
            return user != null ? convertToUserSummaryDTO(user) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get user's followers
     */
    public Page<UserSummaryDTO> getFollowers(String userId, Pageable pageable) {
        try {
            ObjectId userObjectId = new ObjectId(userId);
            Page<FollowEntry> followEntries = followRepository.findByFollowingId(userObjectId, pageable);
            
            List<UserSummaryDTO> followers = followEntries.getContent().stream()
                .map(follow -> {
                    Optional<UserEntry> follower = userRepository.findById(follow.getFollowerId());
                    return follower.map(this::convertToUserSummaryDTO).orElse(null);
                })
                .filter(user -> user != null)
                .collect(Collectors.toList());
            
            return new PageImpl<>(followers, pageable, followEntries.getTotalElements());
        } catch (Exception e) {
            log.error("Error getting followers for user: {}", userId, e);
            return Page.empty(pageable);
        }
    }

    /**
     * Get user's following
     */
    public Page<UserSummaryDTO> getFollowing(String userId, Pageable pageable) {
        try {
            ObjectId userObjectId = new ObjectId(userId);
            Page<FollowEntry> followEntries = followRepository.findByFollowerId(userObjectId, pageable);
            
            List<UserSummaryDTO> following = followEntries.getContent().stream()
                .map(follow -> {
                    Optional<UserEntry> followedUser = userRepository.findById(follow.getFollowingId());
                    return followedUser.map(this::convertToUserSummaryDTO).orElse(null);
                })
                .filter(user -> user != null)
                .collect(Collectors.toList());
            
            return new PageImpl<>(following, pageable, followEntries.getTotalElements());
        } catch (Exception e) {
            log.error("Error getting following for user: {}", userId, e);
            return Page.empty(pageable);
        }
    }

    /**
     * Follow a user
     */
    @Transactional
    public void followUser(String followerId, String userId) {
        try {
            ObjectId followerObjectId = new ObjectId(followerId);
            ObjectId userObjectId = new ObjectId(userId);
            
            // Don't allow self-following
            if (followerObjectId.equals(userObjectId)) {
                throw new IllegalArgumentException("Users cannot follow themselves");
            }
            
            // Check if already following
            if (followRepository.existsByFollowerIdAndFollowingId(followerObjectId, userObjectId)) {
                log.warn("User {} already follows user {}", followerId, userId);
                return;
            }
            
            // Verify both users exist
            if (!userRepository.existsById(followerObjectId) || !userRepository.existsById(userObjectId)) {
                throw new IllegalArgumentException("One or both users do not exist");
            }
            
            // Create follow relationship
            FollowEntry followEntry = FollowEntry.builder()
                .followerId(followerObjectId)
                .followingId(userObjectId)
                .createdAt(LocalDateTime.now())
                .build();
            
            followRepository.save(followEntry);
            log.info("User {} now follows user {}", followerId, userId);
        } catch (Exception e) {
            log.error("Error following user: {}", userId, e);
            throw new RuntimeException("Failed to follow user", e);
        }
    }

    /**
     * Unfollow a user
     */
    @Transactional
    public void unfollowUser(String followerId, String userId) {
        try {
            ObjectId followerObjectId = new ObjectId(followerId);
            ObjectId userObjectId = new ObjectId(userId);
            
            followRepository.deleteByFollowerIdAndFollowingId(followerObjectId, userObjectId);
            log.info("User {} unfollowed user {}", followerId, userId);
        } catch (Exception e) {
            log.error("Error unfollowing user: {}", userId, e);
            throw new RuntimeException("Failed to unfollow user", e);
        }
    }

    /**
     * Check if user follows another user
     */
    public boolean isFollowing(String followerId, String userId) {
        try {
            ObjectId followerObjectId = new ObjectId(followerId);
            ObjectId userObjectId = new ObjectId(userId);
            return followRepository.existsByFollowerIdAndFollowingId(followerObjectId, userObjectId);
        } catch (Exception e) {
            log.error("Error checking follow status", e);
            return false;
        }
    }

    /**
     * Get user statistics
     */
    public UserStatsDTO getUserStats(String userId) {
        try {
            ObjectId userObjectId = new ObjectId(userId);
            
            // Get counts
            long postsCount = postRepository.countByAuthorId(userObjectId);
            long followersCount = followRepository.countByFollowingId(userObjectId);
            long followingCount = followRepository.countByFollowerId(userObjectId);
            long commentsCount = commentRepository.countByAuthorId(userObjectId);
            
            // Get user to get member since date
            Optional<UserEntry> userOpt = userRepository.findById(userObjectId);
            String memberSince = userOpt
                .map(user -> user.getCreatedAt() != null 
                    ? user.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                    : "Unknown")
                .orElse("Unknown");
            
            return UserStatsDTO.builder()
                .postsCount(postsCount)
                .followersCount(followersCount)
                .followingCount(followingCount)
                .commentsCount(commentsCount)
                .likesReceived(0L) // Can be implemented later
                .memberSince(memberSince)
                .build();
        } catch (Exception e) {
            log.error("Error getting user stats for user: {}", userId, e);
            return UserStatsDTO.builder()
                .postsCount(0)
                .followersCount(0)
                .followingCount(0)
                .commentsCount(0)
                .likesReceived(0)
                .memberSince("Unknown")
                .build();
        }
    }

    /**
     * Update user profile
     */
    @Transactional
    public UserSummaryDTO updateProfile(String username, ProfileUpdateRequest profileData) {
        try {
            UserEntry user = userRepository.findByuserName(username);
            if (user == null) {
                throw new IllegalArgumentException("User not found: " + username);
            }
            
            // Update fields if provided
            if (profileData.getFirstName() != null && !profileData.getFirstName().isBlank()) {
                user.setFirstName(profileData.getFirstName());
            }
            if (profileData.getLastName() != null && !profileData.getLastName().isBlank()) {
                user.setLastName(profileData.getLastName());
            }
            if (profileData.getBio() != null) {
                user.setBio(profileData.getBio());
            }
            if (profileData.getLocation() != null) {
                user.setLocation(profileData.getLocation());
            }
            if (profileData.getProfileImageUrl() != null) {
                user.setProfileImageUrl(profileData.getProfileImageUrl());
            }
            
            UserEntry updatedUser = userRepository.save(user);
            log.info("Profile updated for user: {}", username);
            return convertToUserSummaryDTO(updatedUser);
        } catch (Exception e) {
            log.error("Error updating profile for user: {}", username, e);
            throw new RuntimeException("Failed to update profile", e);
        }
    }

    /**
     * Upload profile image
     * Note: This method expects the image URL to be provided.
     * Actual file upload should be handled by MediaService
     */
    public UserSummaryDTO uploadProfileImage(String username, String imageUrl) {
        try {
            UserEntry user = userRepository.findByuserName(username);
            if (user == null) {
                throw new IllegalArgumentException("User not found: " + username);
            }
            
            user.setProfileImageUrl(imageUrl);
            UserEntry updatedUser = userRepository.save(user);
            log.info("Profile image updated for user: {}", username);
            return convertToUserSummaryDTO(updatedUser);
        } catch (Exception e) {
            log.error("Error uploading profile image for user: {}", username, e);
            throw new RuntimeException("Failed to upload profile image", e);
        }
    }

    /**
     * Convert UserEntry to UserSummaryDTO
     */
    private UserSummaryDTO convertToUserSummaryDTO(UserEntry user) {
        return UserSummaryDTO.builder()
                .id(user.getId().toString())
                .userName(user.getUserName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .bio(user.getBio())
                .location(user.getLocation())
                .profileImageUrl(user.getProfileImageUrl())
                .roles(user.getRoles())
                .build();
    }
}

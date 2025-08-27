package org.adnan.travner.service;

import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import org.bson.types.ObjectId;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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
}

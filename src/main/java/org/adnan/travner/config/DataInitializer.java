package org.adnan.travner.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Data initializer to create default admin user if none exists
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeDefaultAdminUser();
    }

    private void initializeDefaultAdminUser() {
        try {
            // List of admin usernames to promote
            List<String> adminUsernames = List.of("admin", "superadmin");
            
            for (String username : adminUsernames) {
                UserEntry existingUser = userRepository.findByuserName(username);
                
                if (existingUser == null && username.equals("admin")) {
                    log.info("No admin user found. Creating default admin user...");
                    
                    // Create default admin user
                    UserEntry adminUser = UserEntry.builder()
                            .userName("admin")
                            .password(passwordEncoder.encode("admin123"))
                            .email("admin@travner.com")
                            .firstName("Admin")
                            .lastName("User")
                            .roles(List.of("USER", "ADMIN"))
                            .active(true)
                            .createdAt(java.time.LocalDateTime.now())
                            .build();
                    
                    userRepository.save(adminUser);
                    log.info("Default admin user created successfully!");
                    log.info("Username: admin");
                    log.info("Password: admin123");
                    log.info("Roles: USER, ADMIN");
                    
                } else if (existingUser != null) {
                    log.info("Checking admin roles for user: {}", username);
                    
                    // Ensure user has ADMIN role
                    if (existingUser.getRoles() == null || !existingUser.getRoles().contains("ADMIN")) {
                        log.info("Promoting user {} to admin role...", username);
                        existingUser.setRoles(List.of("USER", "ADMIN"));
                        userRepository.save(existingUser);
                        log.info("User {} promoted to admin successfully!", username);
                        log.info("Username: {}", username);
                        log.info("Roles: USER, ADMIN");
                    } else {
                        log.info("User {} already has admin role.", username);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to initialize admin users: {}", e.getMessage(), e);
        }
    }
}

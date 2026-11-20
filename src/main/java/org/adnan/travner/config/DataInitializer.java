package org.adnan.travner.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.UserRepository;
import org.adnan.travner.util.IdentityNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Optional one-time bootstrap for the first administrator account.
 *
 * <p>Nothing is created unless BOTH {@code APP_BOOTSTRAP_ADMIN_USERNAME} and
 * {@code APP_BOOTSTRAP_ADMIN_PASSWORD} are supplied, and an admin already
 * existing in the database always wins. Credentials and password values are
 * never logged. Rotate the bootstrap password immediately after first login.</p>
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private static final String USERNAME_PATTERN = "^[a-z0-9_]{3,20}$";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${APP_BOOTSTRAP_ADMIN_USERNAME:}")
    private String bootstrapUsername;

    @Value("${APP_BOOTSTRAP_ADMIN_PASSWORD:}")
    private String bootstrapPassword;

    @Value("${APP_BOOTSTRAP_ADMIN_EMAIL:admin@travner.local}")
    private String bootstrapEmail;

    @Override
    public void run(String... args) {
        if (isBlank(bootstrapUsername) && isBlank(bootstrapPassword)) {
            return;
        }

        if (isBlank(bootstrapUsername) || isBlank(bootstrapPassword)) {
            log.error("Bootstrap admin requires both APP_BOOTSTRAP_ADMIN_USERNAME and APP_BOOTSTRAP_ADMIN_PASSWORD; skipping.");
            return;
        }

        String username = IdentityNormalizer.normalizeUsername(bootstrapUsername);
        if (!username.matches(USERNAME_PATTERN)) {
            log.error("Bootstrap admin username fails validation (3-20 lowercase letters, digits or underscores); skipping.");
            return;
        }

        if (!isStrongPassword(bootstrapPassword)) {
            log.error("Bootstrap admin password fails strength requirements (min 12 chars with upper, lower, digit and symbol); skipping.");
            return;
        }

        try {
            if (userRepository.countAdmins() > 0) {
                log.info("An administrator already exists. Skipping bootstrap admin creation.");
                return;
            }
            if (userRepository.findByuserName(username) != null) {
                log.error("Bootstrap admin username is already taken by a non-admin user; skipping.");
                return;
            }
            if (userRepository.findByEmail(IdentityNormalizer.normalizeEmail(bootstrapEmail)) != null) {
                log.error("Bootstrap admin email is already in use; skipping.");
                return;
            }

            UserEntry admin = UserEntry.builder()
                    .userName(username)
                    .password(passwordEncoder.encode(bootstrapPassword))
                    .email(bootstrapEmail)
                    .firstName("Bootstrap")
                    .lastName("Admin")
                    .roles(List.of("USER", "ADMIN"))
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            userRepository.save(admin);
            // Never log the password or the encoded hash.
            log.info("Bootstrap administrator '{}' created. Rotate its password immediately.", username);
        } catch (Exception e) {
            log.error("Could not create bootstrap administrator: {}", e.getMessage());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 12) {
            return false;
        }
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean symbol = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else {
                symbol = true;
            }
        }
        return upper && lower && digit && symbol;
    }
}

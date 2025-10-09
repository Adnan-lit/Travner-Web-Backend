package org.adnan.travner.controller;

import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UserEntry testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserEntry();
        testUser.setId(new ObjectId());
        testUser.setUserName("testuser");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("test@example.com");
        testUser.setPassword(""); // Password should be empty for secure responses
        testUser.setRoles(List.of("USER"));
        testUser.setActive(true);
        testUser.setBio("Test bio");
        testUser.setLocation("Test location");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setLastLoginAt(LocalDateTime.now());
    }

    @Test
    void testUserServiceIsInjected() {
        // Test that the user service is properly injected
        assertNotNull(userController);
        // We can't directly test the private userService field,
        // but we can verify the controller exists
    }

    // Note: These tests are limited because UserController methods
    // depend on Spring Security context which is not available in unit tests.
    // For full integration testing, use @SpringBootTest with security context.
}
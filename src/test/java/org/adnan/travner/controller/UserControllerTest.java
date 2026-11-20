package org.adnan.travner.controller;

import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.exception.ResourceNotFoundException;
import org.adnan.travner.security.UserPrincipal;
import org.adnan.travner.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.bson.types.ObjectId;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    // P2-01: the JWT subject (UserPrincipal id) must be passed to the service,
    // never the display username.

    @Test
    void followUser_passesPrincipalIdNotUsername() {
        Authentication auth = jwtAuthentication();
        String targetId = new ObjectId().toHexString();

        userController.followUser(targetId, auth);

        verify(userService).followUser(testUser.getId().toHexString(), targetId);
    }

    @Test
    void unfollowUser_passesPrincipalIdNotUsername() {
        Authentication auth = jwtAuthentication();
        String targetId = new ObjectId().toHexString();

        userController.unfollowUser(targetId, auth);

        verify(userService).unfollowUser(testUser.getId().toHexString(), targetId);
    }

    @Test
    void isFollowing_passesPrincipalIdNotUsername() {
        Authentication auth = jwtAuthentication();
        String targetId = new ObjectId().toHexString();

        userController.isFollowing(targetId, auth);

        verify(userService).isFollowing(testUser.getId().toHexString(), targetId);
    }

    @Test
    void followUser_resolvesNonJwtPrincipalByUsername() {
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "ignored", List.of());
        when(userService.getByUsername("testuser")).thenReturn(testUser);
        String targetId = new ObjectId().toHexString();

        userController.followUser(targetId, auth);

        verify(userService).followUser(testUser.getId().toHexString(), targetId);
    }

    @Test
    void followUser_rejectsNonJwtPrincipalThatCannotBeResolved() {
        Authentication auth = new UsernamePasswordAuthenticationToken("ghost", "ignored", List.of());
        when(userService.getByUsername("ghost")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class,
                () -> userController.followUser(new ObjectId().toHexString(), auth));
    }

    @Test
    void followUser_rejectsAnonymousPrincipal() {
        assertThrows(ResourceNotFoundException.class,
                () -> userController.followUser(new ObjectId().toHexString(), null));
    }

    /** A principal as produced by JwtAuthenticationConverter: subject = user ID, name = username. */
    private Authentication jwtAuthentication() {
        UserPrincipal principal = new UserPrincipal(
                testUser.getId().toHexString(), "testuser", null, "test@example.com",
                List.of("USER"), true);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
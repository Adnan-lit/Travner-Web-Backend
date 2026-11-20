package org.adnan.travner.service;

import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.exception.DuplicateUserException;
import org.adnan.travner.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.bson.types.ObjectId;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthService authService;

    @InjectMocks
    private UserService userService;

    private UserEntry testUser;
    private final PasswordEncoder realPasswordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        testUser = new UserEntry();
        testUser.setId(new ObjectId());
        testUser.setUserName("testuser");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPassword("encodedpassword");
        testUser.setEmail("test@example.com");
        testUser.setRoles(List.of("USER"));
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testSaveNewUser_Success() {
        // Given
        UserEntry newUser = new UserEntry();
        newUser.setUserName("newuser");
        newUser.setPassword("plainpassword");
        when(passwordEncoder.encode("plainpassword")).thenReturn("$2a$10$encodedpassword");
        when(userRepository.save(any(UserEntry.class))).thenReturn(newUser);

        // When
        userService.saveNewUser(newUser);

        // Then
        verify(userRepository).save(any(UserEntry.class));
        verify(passwordEncoder).encode("plainpassword");
        assertEquals("$2a$10$encodedpassword", newUser.getPassword());
        assertEquals(List.of("USER"), newUser.getRoles());
        assertTrue(newUser.isActive());
        assertNotNull(newUser.getCreatedAt());
    }

    @Test
    void saveNewUser_normalizesIdentityBeforeDuplicateChecksAndStorage() {
        UserEntry newUser = new UserEntry();
        newUser.setUserName("  Alice_01  ");
        newUser.setEmail("  ALICE@Example.COM ");
        newUser.setPassword("plainpassword");
        when(userRepository.findByuserName("alice_01")).thenReturn(null);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(null);
        when(passwordEncoder.encode("plainpassword")).thenReturn("encoded");

        userService.saveNewUser(newUser);

        ArgumentCaptor<UserEntry> captor = ArgumentCaptor.forClass(UserEntry.class);
        verify(userRepository).save(captor.capture());
        assertEquals("alice_01", captor.getValue().getUserName());
        assertEquals("alice@example.com", captor.getValue().getEmail());
        verify(userRepository).findByuserName("alice_01");
        verify(userRepository).findByEmail("alice@example.com");
    }

    @Test
    void saveNewUser_duplicateEmailThrowsGenericExceptionWithoutEncodingPassword() {
        UserEntry newUser = new UserEntry();
        newUser.setUserName("alice_02");
        newUser.setEmail(" ALICE@EXAMPLE.COM ");
        newUser.setPassword("plainpassword");
        when(userRepository.findByuserName("alice_02")).thenReturn(null);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(testUser);

        DuplicateUserException ex = assertThrows(DuplicateUserException.class,
                () -> userService.saveNewUser(newUser));

        assertEquals("Username or email already exists", ex.getMessage());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void saveNewUser_translatesConcurrentDuplicateKeyIntoGenericException() {
        UserEntry newUser = new UserEntry();
        newUser.setUserName("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("plainpassword");
        when(userRepository.findByuserName("newuser")).thenReturn(null);
        when(userRepository.findByEmail("new@example.com")).thenReturn(null);
        when(passwordEncoder.encode("plainpassword")).thenReturn("encoded");
        when(userRepository.save(any(UserEntry.class)))
                .thenThrow(new DuplicateKeyException("E11000 duplicate key"));

        DuplicateUserException ex = assertThrows(DuplicateUserException.class,
                () -> userService.saveNewUser(newUser));

        assertEquals("Username or email already exists", ex.getMessage());
    }

    @Test
    void getByUsername_normalizesMixedCaseLookup() {
        when(userRepository.findByuserName("testuser")).thenReturn(testUser);

        assertEquals(testUser, userService.getByUsername("  TESTUSER  "));
        verify(userRepository).findByuserName("testuser");
    }

    @Test
    void testGetByUsername_Success() {
        // Given
        when(userRepository.findByuserName("testuser")).thenReturn(testUser);

        // When
        UserEntry result = userService.getByUsername("testuser");

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUserName());
        verify(userRepository).findByuserName("testuser");
    }

    @Test
    void testGetByUsernameSecure_Success() {
        // Given
        when(userRepository.findByuserName("testuser")).thenReturn(testUser);

        // When
        UserEntry result = userService.getByUsernameSecure("testuser");

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUserName());
        assertEquals("", result.getPassword()); // Password should be removed for security
        verify(userRepository).findByuserName("testuser");
    }

    @Test
    void testDeleteUser_Success() {
        // Given
        doNothing().when(userRepository).delete(testUser);

        // When
        boolean result = userService.deleteUser(testUser);

        // Then
        assertTrue(result);
        verify(userRepository).delete(testUser);
    }

    @Test
    void testDeleteUser_NullUser() {
        // When
        boolean result = userService.deleteUser(null);

        // Then
        assertFalse(result);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void testDeleteUserById_Success() {
        // Given
        ObjectId userId = new ObjectId();
        doNothing().when(userRepository).deleteById(userId);

        // When
        boolean result = userService.deleteUserById(userId.toString());

        // Then
        assertTrue(result);
        verify(userRepository).deleteById(userId);
    }

    @Test
    void testDeleteUserById_InvalidId() {
        // When
        boolean result = userService.deleteUserById("invalid-id");

        // Then
        assertFalse(result);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void testUpdateUserRoles_Success() {
        // Given
        List<String> newRoles = List.of("USER", "ADMIN");
        when(userRepository.findByuserName("testuser")).thenReturn(testUser);
        when(userRepository.save(any(UserEntry.class))).thenReturn(testUser);

        // When
        boolean result = userService.updateUserRoles("testuser", newRoles);

        // Then
        assertTrue(result);
        assertEquals(newRoles, testUser.getRoles());
        verify(userRepository).save(testUser);
    }

    @Test
    void testResetUserPassword_Success() {
        // Given
        when(userRepository.findByuserName("testuser")).thenReturn(testUser);
        when(passwordEncoder.encode("newpassword")).thenReturn("$2a$10$newencodedpassword");
        when(userRepository.save(any(UserEntry.class))).thenReturn(testUser);

        // When
        boolean result = userService.resetUserPassword("testuser", "newpassword");

        // Then
        assertTrue(result);
        verify(passwordEncoder).encode("newpassword");
        assertEquals("$2a$10$newencodedpassword", testUser.getPassword());
        verify(userRepository).save(testUser);
        verify(authService).revokeAllForUser(testUser.getId().toHexString());
    }

    @Test
    void testGetUserCount() {
        // Given
        when(userRepository.count()).thenReturn(5L);

        // When
        long result = userService.getUserCount();

        // Then
        assertEquals(5L, result);
        verify(userRepository).count();
    }

    @Test
    void testPromoteUserToAdmin_Success() {
        // Given
        when(userRepository.findByuserName("testuser")).thenReturn(testUser);
        when(userRepository.save(any(UserEntry.class))).thenReturn(testUser);

        // When
        boolean result = userService.promoteUserToAdmin("testuser");

        // Then
        assertTrue(result);
        assertTrue(testUser.getRoles().contains("ADMIN"));
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateUserProfile_Success() {
        // Given
        when(userRepository.findByuserName("testuser")).thenReturn(testUser);
        when(userRepository.save(any(UserEntry.class))).thenReturn(testUser);

        // When
        boolean result = userService.updateUserProfile("testuser", "NewFirst", "NewLast", "new@email.com");

        // Then
        assertTrue(result);
        assertEquals("NewFirst", testUser.getFirstName());
        assertEquals("NewLast", testUser.getLastName());
        assertEquals("new@email.com", testUser.getEmail());
        verify(userRepository).save(testUser);
    }

    @Test
    void testChangeUserPassword_Success() {
        // Given
        String plainCurrentPassword = "currentpass";
        String encodedCurrentPassword = "$2a$10$encodedcurrentpass";
        testUser.setPassword(encodedCurrentPassword);

        when(userRepository.findByuserName("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches(plainCurrentPassword, encodedCurrentPassword)).thenReturn(true);
        when(passwordEncoder.encode("newpassword")).thenReturn("$2a$10$newencodedpassword");
        when(userRepository.save(any(UserEntry.class))).thenReturn(testUser);

        // When
        boolean result = userService.changeUserPassword("testuser", plainCurrentPassword, "newpassword");

        // Then
        assertTrue(result);
        verify(passwordEncoder).matches(plainCurrentPassword, encodedCurrentPassword);
        verify(passwordEncoder).encode("newpassword");
        verify(userRepository).save(testUser);
    }

    @Test
    void testIsUsernameAvailable_Available() {
        // Given
        when(userRepository.findByuserName("availableuser")).thenReturn(null);

        // When
        boolean result = userService.isUsernameAvailable("availableuser");

        // Then
        assertTrue(result);
        verify(userRepository).findByuserName("availableuser");
    }

    @Test
    void testIsUsernameAvailable_NotAvailable() {
        // Given
        when(userRepository.findByuserName("testuser")).thenReturn(testUser);

        // When
        boolean result = userService.isUsernameAvailable("testuser");

        // Then
        assertFalse(result);
        verify(userRepository).findByuserName("testuser");
    }

    @Test
    void testUpdateUserPartial_Success() {
        // Given
        Map<String, Object> updates = Map.of(
                "firstName", "UpdatedFirst",
                "bio", "Updated bio");
        when(userRepository.findByuserName("testuser")).thenReturn(testUser);
        when(userRepository.save(any(UserEntry.class))).thenReturn(testUser);

        // When
        boolean result = userService.updateUserPartial("testuser", updates);

        // Then
        assertTrue(result);
        assertEquals("UpdatedFirst", testUser.getFirstName());
        assertEquals("Updated bio", testUser.getBio());
        verify(userRepository).save(testUser);
    }

    @Test
    void testGetUserByEmail_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

        // When
        UserEntry result = userService.getUserByEmail("test@example.com");

        // Then
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void testUpdateLastLogin() {
        // Given
        when(userRepository.findByuserName("testuser")).thenReturn(testUser);
        when(userRepository.save(any(UserEntry.class))).thenReturn(testUser);

        // When
        userService.updateLastLogin("testuser");

        // Then
        assertNotNull(testUser.getLastLoginAt());
        verify(userRepository).save(testUser);
    }

    @Test
    void testSetUserActiveStatus_Success() {
        // Given
        when(userRepository.findByuserName("testuser")).thenReturn(testUser);
        when(userRepository.save(any(UserEntry.class))).thenReturn(testUser);

        // When
        boolean result = userService.setUserActiveStatus("testuser", false);

        // Then
        assertTrue(result);
        assertFalse(testUser.isActive());
        verify(userRepository).save(testUser);
    }
}


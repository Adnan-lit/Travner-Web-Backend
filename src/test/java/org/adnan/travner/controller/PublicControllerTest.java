package org.adnan.travner.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.exception.DuplicateUserException;
import org.adnan.travner.service.UserService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PublicControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        PublicController controller = new PublicController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void register_normalizesIdentityAndReturnsCanonicalUsername() throws Exception {
        when(userService.isUsernameAvailable("alice_01")).thenReturn(true);
        when(userService.getByUsernameSecure("alice_01")).thenReturn(createdUser());

        String body = mockMvc.perform(post("/api/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userName":"  Alice_01  ","email":" ALICE@Example.COM ",
                                 "password":"Password1","firstName":"Alice","lastName":"Example"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode response = objectMapper.readTree(body);
        assertEquals("alice_01", response.at("/data/userName").asText());
        assertEquals("alice@example.com", response.at("/data/email").asText());
        verify(userService).isUsernameAvailable("alice_01");
        verify(userService).saveNewUser(any(UserEntry.class));
    }

    @Test
    void register_duplicateEmailReturnsGenericConflictWithoutLeakingDetails() throws Exception {
        when(userService.isUsernameAvailable("alice_01")).thenReturn(true);
        doThrow(new DuplicateUserException()).when(userService).saveNewUser(any(UserEntry.class));

        String body = mockMvc.perform(post("/api/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userName":"Alice_01","email":"alice@example.com",
                                 "password":"Password1","firstName":"Alice","lastName":"Example"}
                                """))
                .andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();

        JsonNode response = objectMapper.readTree(body);
        assertFalse(response.get("success").asBoolean());
        assertEquals("Username or email already exists", response.get("message").asText());
    }

    @Test
    void register_unexpectedFailureDoesNotExposeExceptionMessage() throws Exception {
        when(userService.isUsernameAvailable("alice_01")).thenReturn(true);
        doThrow(new IllegalStateException("secret connection string"))
                .when(userService).saveNewUser(any(UserEntry.class));

        String body = mockMvc.perform(post("/api/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userName":"Alice_01","email":"alice@example.com",
                                 "password":"Password1","firstName":"Alice","lastName":"Example"}
                                """))
                .andExpect(status().isInternalServerError())
                .andReturn().getResponse().getContentAsString();

        assertEquals("Registration failed", objectMapper.readTree(body).get("message").asText());
    }

    @Test
    void checkUsername_delegatesToNormalizingService() throws Exception {
        when(userService.isUsernameAvailable(anyString())).thenReturn(true);

        mockMvc.perform(get("/api/public/check-username/Alice_01"))
                .andExpect(status().isOk());

        verify(userService).isUsernameAvailable("Alice_01");
        verify(userService, never()).getByUsernameSecure(anyString());
    }

    private UserEntry createdUser() {
        UserEntry user = new UserEntry();
        user.setId(new ObjectId());
        user.setUserName("alice_01");
        user.setEmail("alice@example.com");
        user.setFirstName("Alice");
        user.setLastName("Example");
        user.setRoles(List.of("USER"));
        return user;
    }
}

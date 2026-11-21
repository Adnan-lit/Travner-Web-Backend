package org.adnan.travner.controller;

import org.adnan.travner.config.ClientAddressResolver;
import org.adnan.travner.config.RateLimitDecision;
import org.adnan.travner.config.RateLimiter;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.PostRepository;
import org.adnan.travner.repository.UserRepository;
import org.adnan.travner.service.PostService;
import org.adnan.travner.service.ProductService;
import org.adnan.travner.service.UserService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Regression tests for P2-11: admin endpoints never serialize the UserEntry entity. */
@WebMvcTest(AdminController.class)
@Import(org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class)
class AdminUserPiiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private PostRepository postRepository;
    @MockBean private UserRepository userRepository;
    @MockBean private MongoTemplate mongoTemplate;
    @MockBean private UserService userService;
    @MockBean private PostService postService;
    @MockBean private ProductService productService;
    @MockBean private PasswordEncoder passwordEncoder;
    @MockBean private RateLimiter rateLimiter;
    @MockBean private ClientAddressResolver clientAddressResolver;

    @BeforeEach
    void allowRequests() {
        when(rateLimiter.isApiAllowed(anyString())).thenReturn(true);
        when(rateLimiter.allowApi(anyString())).thenReturn(RateLimitDecision.permit());
        when(clientAddressResolver.resolveClientIp(any())).thenReturn("127.0.0.1");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getUserByUsernameReturnsSafeDtoWithoutPasswordHash() throws Exception {
        when(userService.getByUsername("alice_01")).thenReturn(user());

        mockMvc.perform(get("/api/admin/users/alice_01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userName").value("alice_01"))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.secretHash").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listUsersReturnsSafeDtosWithoutPasswordHash() throws Exception {
        when(mongoTemplate.count(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(UserEntry.class)))
                .thenReturn(1L);
        when(mongoTemplate.find(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(UserEntry.class)))
                .thenReturn(List.of(user()));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].userName").value("alice_01"))
                .andExpect(jsonPath("$.data.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].secretHash").doesNotExist());
    }

    private static UserEntry user() {
        UserEntry user = new UserEntry();
        user.setId(new ObjectId());
        user.setUserName("alice_01");
        user.setEmail("alice@example.com");
        user.setPassword("$2a$10$secret-hash");
        user.setFirstName("Alice");
        user.setLastName("Example");
        user.setRoles(List.of("USER"));
        user.setActive(true);
        return user;
    }
}

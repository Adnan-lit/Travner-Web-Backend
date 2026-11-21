package org.adnan.travner.controller;

import org.adnan.travner.config.ClientAddressResolver;
import org.adnan.travner.config.RateLimitDecision;
import org.adnan.travner.config.RateLimiter;
import org.adnan.travner.repository.PostRepository;
import org.adnan.travner.repository.UserRepository;
import org.adnan.travner.service.PostService;
import org.adnan.travner.service.ProductService;
import org.adnan.travner.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class)
class AdminBulkOperationErrorTest {

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
    void bulkOperationDoesNotLeakExceptionMessage() throws Exception {
        when(userService.getByUsername("bob"))
                .thenThrow(new IllegalStateException("mongodb://user:secret@internal-host"));

        mockMvc.perform(post("/api/admin/users/bulk")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operation\":\"activate\",\"usernames\":[\"bob\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.failureCount").value(1))
                .andExpect(jsonPath("$.data.errors[0]", containsString("bob")))
                .andExpect(jsonPath("$.data.errors[0]", not(containsString("secret"))))
                .andExpect(jsonPath("$.data.errors[0]", not(containsString("internal-host"))));
    }
}

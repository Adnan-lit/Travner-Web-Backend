package org.adnan.travner.security;

import org.adnan.travner.config.CorsConfig;
import org.adnan.travner.config.JwtConfig;
import org.adnan.travner.config.RateLimiter;
import org.adnan.travner.config.SpringSecurity;
import org.adnan.travner.controller.AdminController;
import org.adnan.travner.controller.CartController;
import org.adnan.travner.controller.ChatController;
import org.adnan.travner.controller.ItineraryController;
import org.adnan.travner.controller.MediaController;
import org.adnan.travner.controller.OrderController;
import org.adnan.travner.controller.UserController;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.service.CartService;
import org.adnan.travner.service.ChatService;
import org.adnan.travner.service.ItineraryService;
import org.adnan.travner.service.MediaService;
import org.adnan.travner.service.OrderService;
import org.adnan.travner.service.PostService;
import org.adnan.travner.service.ProductService;
import org.adnan.travner.service.UserDetailsServiceImpl;
import org.adnan.travner.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.stream.Stream;

import static org.adnan.travner.security.JwtTestTokenFactory.ADMIN;
import static org.adnan.travner.security.JwtTestTokenFactory.ADMIN_ID;
import static org.adnan.travner.security.JwtTestTokenFactory.USER_A;
import static org.adnan.travner.security.JwtTestTokenFactory.USER_A_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auth/authz matrix: the same protected endpoints are exercised as anonymous,
 * a normal user, and an admin. Complements the object-level ownership matrix
 * with service mocks (see {@code ObjectLevelAuthzMatrixTest}).
 */
@WebMvcTest(controllers = {
        AdminController.class, CartController.class, ChatController.class,
        ItineraryController.class, MediaController.class, OrderController.class,
        UserController.class
})
@AutoConfigureMockMvc(addFilters = true)
@Import({SpringSecurity.class, JwtConfig.class, CorsConfig.class})
class EndpointAuthzMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockBean
    private RateLimiter rateLimiter;
    @MockBean
    private UserDetailsServiceImpl userDetailsService;
    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private org.adnan.travner.repository.PostRepository postRepository;
    @MockBean
    private org.adnan.travner.repository.UserRepository userRepository;
    @MockBean
    private MongoTemplate mongoTemplate;
    @MockBean
    private UserService userService;
    @MockBean
    private PostService postService;
    @MockBean
    private ProductService productService;

    @MockBean
    private CartService cartService;
    @MockBean
    private ChatService chatService;
    @MockBean
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    @MockBean
    private ItineraryService itineraryService;
    @MockBean
    private MediaService mediaService;
    @MockBean
    private OrderService orderService;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        when(rateLimiter.isApiAllowed(anyString())).thenReturn(true);
        when(rateLimiter.isChatAllowed(anyString())).thenReturn(true);
        when(rateLimiter.isUploadAllowed(anyString())).thenReturn(true);
        when(rateLimiter.isLoginAllowed(anyString())).thenReturn(true);
        when(mongoTemplate.count(any(Query.class), eq(UserEntry.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(UserEntry.class))).thenReturn(List.of());
        when(orderService.getAllOrders()).thenReturn(List.of());
        when(chatService.getUserConversations(anyString(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(userService.searchUsers(anyString(), any())).thenReturn(new PageImpl<>(List.of()));

        userToken = "Bearer " + JwtTestTokenFactory.token(
                jwtEncoder, USER_A_ID, USER_A, "USER");
        adminToken = "Bearer " + JwtTestTokenFactory.token(
                jwtEncoder, ADMIN_ID, ADMIN, "ADMIN");
    }

    static Stream<Endpoint> adminOnlyEndpoints() {
        return Stream.of(
                new Endpoint("GET /api/admin/users", HttpMethod.GET, "/api/admin/users", false),
                new Endpoint("GET /api/market/orders/admin", HttpMethod.GET, "/api/market/orders/admin", false));
    }

    @ParameterizedTest(name = "admin-only {0}")
    @MethodSource("adminOnlyEndpoints")
    void adminOnlyEndpoint_anonymous401_user403_admin200(Endpoint endpoint) throws Exception {
        mockMvc.perform(requestFor(endpoint))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(requestFor(endpoint).header("Authorization", userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(requestFor(endpoint).header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    static Stream<Endpoint> userRoleEndpoints() {
        return Stream.of(
                new Endpoint("GET /api/chat/conversations", HttpMethod.GET, "/api/chat/conversations", false),
                new Endpoint("GET /api/users/search", HttpMethod.GET, "/api/users/search?q=alice", false),
                new Endpoint("POST /api/media/upload", HttpMethod.POST, "/api/media/upload", true),
                new Endpoint("POST /api/market/orders/{id}/fulfill", HttpMethod.POST,
                        "/api/market/orders/ord1/fulfill", false),
                new Endpoint("POST /api/users/{id}/follow", HttpMethod.POST,
                        "/api/users/507f191e810c19729de860ea/follow", false));
    }

    @ParameterizedTest(name = "user-role {0}")
    @MethodSource("userRoleEndpoints")
    void userRoleEndpoint_anonymous401_user2xx_admin2xx(Endpoint endpoint) throws Exception {
        mockMvc.perform(requestFor(endpoint))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(requestFor(endpoint).header("Authorization", userToken))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(requestFor(endpoint).header("Authorization", adminToken))
                .andExpect(status().is2xxSuccessful());
    }

    @ParameterizedTest(name = "invalid bearer: {0}")
    @ValueSource(strings = {
            "Basic YWxpY2U6cGFzc3dvcmQ=",
            "Bearer not-a-jwt",
            "Bearer eyJhbGciOiJSUzI1NiJ9.bad.signature"
    })
    void invalidAuthorizationHeader_isRejectedWith401(String header) throws Exception {
        mockMvc.perform(get("/api/chat/conversations").header("Authorization", header))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "signed but invalid token: {0}")
    @ValueSource(strings = {"expired", "wrongIssuer"})
    void signedButInvalidToken_isRejectedWith401(String kind) throws Exception {
        String token = switch (kind) {
            case "expired" -> JwtTestTokenFactory.expired(jwtEncoder, USER_A_ID, USER_A);
            case "wrongIssuer" -> JwtTestTokenFactory.wrongIssuer(jwtEncoder, USER_A_ID, USER_A);
            default -> throw new IllegalArgumentException(kind);
        };

        mockMvc.perform(get("/api/chat/conversations").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signedTokenWithoutRoles_usesDocumentedUserRoleFallback() throws Exception {
        // Known behaviour (tracked in PLAN.md): UserPrincipal maps an empty roles
        // claim to ROLE_USER, so a role-less signed token is still authenticated.
        String token = JwtTestTokenFactory.withoutRoles(jwtEncoder, USER_A_ID, USER_A);

        mockMvc.perform(get("/api/chat/conversations").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private MockHttpServletRequestBuilder requestFor(Endpoint endpoint) {
        if (endpoint.multipart()) {
            return multipart(endpoint.path())
                    .file(new MockMultipartFile("file", "probe.txt", "text/plain", new byte[]{1}));
        }
        return switch (endpoint.method().name()) {
            case "GET" -> get(endpoint.path());
            case "POST" -> post(endpoint.path());
            case "DELETE" -> delete(endpoint.path());
            default -> throw new IllegalArgumentException(endpoint.method().name());
        };
    }

    private record Endpoint(String name, HttpMethod method, String path, boolean multipart) {
    }
}

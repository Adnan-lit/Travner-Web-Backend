package org.adnan.travner.security;

import org.adnan.travner.config.JwtConfig;
import org.adnan.travner.config.ClientAddressResolver;
import org.adnan.travner.config.RateLimitDecision;
import org.adnan.travner.config.RateLimiter;
import org.adnan.travner.config.SpringSecurity;
import org.adnan.travner.controller.PostController;
import org.adnan.travner.dto.PostDTO;
import org.adnan.travner.service.MediaService;
import org.adnan.travner.service.PostService;
import org.adnan.travner.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifies the P1-02 switch from HTTP Basic to bearer-token authentication. */
@WebMvcTest(controllers = PostController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SpringSecurity.class, JwtConfig.class})
class JwtAuthenticationTest {

    private static final String USER_ID = "507f1f77bcf86cd799439011";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockBean
    private PostService postService;

    @MockBean
    private MediaService mediaService;

    @MockBean
    private RateLimiter rateLimiter;

    @MockBean
    private ClientAddressResolver clientAddressResolver;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        when(rateLimiter.isApiAllowed(any(String.class))).thenReturn(true);
        when(rateLimiter.allowApi(any(String.class))).thenReturn(RateLimitDecision.permit());
        when(clientAddressResolver.resolveClientIp(any())).thenReturn("127.0.0.1");
    }

    @Test
    void converter_exposesUserIdAndDisplayUsernameSeparately() {
        org.springframework.security.oauth2.jwt.Jwt jwt =
                org.springframework.security.oauth2.jwt.Jwt.withTokenValue("token")
                        .header("alg", "RS256")
                        .subject(USER_ID)
                        .claim("username", "alice")
                        .claim("roles", List.of("USER"))
                        .build();

        var authentication = new JwtAuthenticationConverter().convert(jwt);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        assertEquals(USER_ID, principal.getId(), "subject must be the user ID (D2)");
        assertEquals("alice", principal.getUsername(), "username is display-only");
        assertEquals("alice", authentication.getName(), "controllers keep a stable display name");
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void bearerTokenWithUserRole_allowsProtectedWrite() throws Exception {
        when(postService.createPost(eq("testuser"), any())).thenReturn(PostDTO.builder().id("1").build());

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + tokenFor("testuser", List.of("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"t","content":"Test content","tags":["x"],"published":true}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void tokenWithoutRequiredRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + tokenFor("testuser", List.of("USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void httpBasicHeader_isNotAccepted() throws Exception {
        String basic = Base64.getEncoder().encodeToString("testuser:password".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Basic " + basic)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"content\":\"c\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousWrite_isStillUnauthorized() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"content\":\"c\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorMetrics_requiresAdminRole() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .header("Authorization", "Bearer " + tokenFor("testuser", List.of("USER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorHealth_staysPublicForUptimeChecks() throws Exception {
        // This slice has no actuator handler, so only assert that the filter
        // chain did not challenge the anonymous request (401/403).
        mockMvc.perform(get("/actuator/health"))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertFalse(
                        result.getResponse().getStatus() == 401
                                || result.getResponse().getStatus() == 403));
    }

    private String tokenFor(String username, List<String> roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("https://travner.local")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .subject(USER_ID)
                .claim("username", username)
                .claim("roles", roles)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
    }
}

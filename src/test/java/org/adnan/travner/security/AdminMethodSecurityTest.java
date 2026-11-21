package org.adnan.travner.security;

import org.adnan.travner.config.JwtConfig;
import org.adnan.travner.config.ClientAddressResolver;
import org.adnan.travner.config.RateLimitDecision;
import org.adnan.travner.config.RateLimiter;
import org.adnan.travner.config.SpringSecurity;
import org.adnan.travner.controller.OrderController;
import org.adnan.travner.service.OrderService;
import org.adnan.travner.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifies @PreAuthorize on admin-only order endpoints. */
@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SpringSecurity.class, JwtConfig.class})
class AdminMethodSecurityTest {

    private static final String USER_ID = "507f1f77bcf86cd799439011";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockBean
    private OrderService orderService;

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
    void userCannotListAllOrders() throws Exception {
        mockMvc.perform(get("/api/market/orders/admin")
                        .header("Authorization", "Bearer " + tokenFor("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListAllOrders() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of());

        mockMvc.perform(get("/api/market/orders/admin")
                        .header("Authorization", "Bearer " + tokenFor("ADMIN")))
                .andExpect(status().isOk());
    }

    private String tokenFor(String role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("https://travner.local")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .subject(USER_ID)
                .claim("username", "alice")
                .claim("roles", List.of(role))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
    }
}

package org.adnan.travner.controller;

import org.adnan.travner.config.RateLimiter;
import org.adnan.travner.config.SpringSecurity;
import org.adnan.travner.dto.auth.AuthResponse;
import org.adnan.travner.dto.auth.AuthUser;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.security.UserPrincipal;
import org.adnan.travner.service.AuthService;
import org.adnan.travner.service.UserDetailsServiceImpl;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(SpringSecurity.class)
class AuthControllerTest {

    private static final String LOGIN_BODY = "{\"username\":\"alice\",\"password\":\"correct\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private RateLimiter rateLimiter;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    private UserEntry user;
    private AuthResponse response;

    @BeforeEach
    void setUp() {
        when(rateLimiter.isApiAllowed(anyString())).thenReturn(true);
        when(rateLimiter.isLoginAllowed(anyString())).thenReturn(true);

        user = UserEntry.builder()
                .id(new ObjectId())
                .userName("alice")
                .email("alice@example.com")
                .roles(List.of("USER"))
                .active(true)
                .build();
        response = AuthResponse.builder()
                .accessToken("signed-access-token")
                .tokenType("Bearer")
                .expiresIn(900)
                .user(AuthUser.builder()
                        .id(user.getId().toHexString())
                        .username("alice")
                        .email("alice@example.com")
                        .roles(List.of("USER"))
                        .build())
                .build();
    }

    @Test
    void login_isAnonymousAndSetsHardenedRefreshCookie() throws Exception {
        when(authService.login("alice", "correct"))
                .thenReturn(new AuthService.AuthResult("signed-access-token", "raw-refresh-secret", user));
        when(authService.toResponse(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed-access-token"))
                .andExpect(jsonPath("$.user.username").value("alice"))
                .andExpect(jsonPath("$.user.id").value(user.getId().toHexString()))
                .andExpect(jsonPath("$", not(containsString("raw-refresh-secret"))))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("travner_refresh=raw-refresh-secret")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/auth")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")));
    }

    @Test
    void login_withBadCredentials_returnsGeneric401() throws Exception {
        when(authService.login("alice", "wrong"))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void login_whenRateLimited_returns429WithoutAuthenticating() throws Exception {
        when(rateLimiter.isLoginAllowed(anyString())).thenReturn(false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.traceId").exists());

        Mockito.verifyNoInteractions(authService);
    }

    @Test
    void refresh_rotatesCookie() throws Exception {
        when(authService.refresh("old-refresh"))
                .thenReturn(new AuthService.AuthResult("new-access", "new-refresh", user));
        when(authService.toResponse(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh").cookie(
                        new jakarta.servlet.http.Cookie("travner_refresh", "old-refresh")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("travner_refresh=new-refresh")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));
    }

    @Test
    void refresh_withInvalidCookie_returns401AndClearsCookie() throws Exception {
        when(authService.refresh(anyString())).thenThrow(new BadCredentialsException("Invalid refresh token"));

        mockMvc.perform(post("/api/auth/refresh").cookie(
                        new jakarta.servlet.http.Cookie("travner_refresh", "stolen")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    @Test
    void logout_revokesTokenAndClearsCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout").cookie(
                        new jakarta.servlet.http.Cookie("travner_refresh", "to-revoke")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        verify(authService).logout("to-revoke");
    }

    @Test
    void me_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void me_returnsCurrentPrincipal() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    void me_withJwtPrincipal_exposesUserId() throws Exception {
        UserPrincipal principal = new UserPrincipal(
                user.getId().toHexString(), "alice", null, "alice@example.com", List.of("USER"), true);

        mockMvc.perform(get("/api/auth/me").with(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(
                                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                        principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toHexString()))
                .andExpect(jsonPath("$.username").value("alice"));
    }
}

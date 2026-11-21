package org.adnan.travner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.adnan.travner.config.ClientAddressResolver;
import org.adnan.travner.config.CorrelationIdFilter;
import org.adnan.travner.config.RateLimitDecision;
import org.adnan.travner.config.RateLimiter;
import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.dto.auth.AuthResponse;
import org.adnan.travner.dto.auth.AuthUser;
import org.adnan.travner.dto.auth.LoginRequest;
import org.adnan.travner.exception.ErrorCode;
import org.adnan.travner.exception.ExceptionResponses;
import org.adnan.travner.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Login / refresh / logout and current-user endpoints (decision D1/D2). */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Access token and refresh cookie endpoints")
public class AuthController {

    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    private final AuthService authService;
    private final RateLimiter rateLimiter;
    private final ClientAddressResolver clientAddressResolver;

    @Value("${app.jwt.refresh-cookie-name:travner_refresh}")
    private String cookieName;

    @Value("${app.jwt.refresh-cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${app.jwt.refresh-token-ttl-days:7}")
    private long refreshTokenTtlDays;

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Returns a bearer access token and sets an HttpOnly refresh cookie")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest) {
        RateLimitDecision decision = rateLimiter.allowLogin(clientKey(httpRequest));
        if (!decision.allowed()) {
            return rateLimitedResponse("Too many login attempts. Please try again later.", decision.retryAfterSeconds());
        }
        try {
            AuthService.AuthResult result = authService.login(request.getUsername(), request.getPassword());
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.refreshToken()).toString())
                    .body(authService.toResponse(result));
        } catch (BadCredentialsException e) {
            // Generic response: never disclose which factor was wrong.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Rotates the refresh cookie and returns a new access token")
    public ResponseEntity<?> refresh(
            @CookieValue(name = "${app.jwt.refresh-cookie-name:travner_refresh}", required = false) String refreshCookie,
            HttpServletRequest httpRequest) {
        RateLimitDecision decision = rateLimiter.allowRefresh(clientKey(httpRequest));
        if (!decision.allowed()) {
            return rateLimitedResponse("Too many refresh attempts. Please try again later.", decision.retryAfterSeconds());
        }
        try {
            AuthService.AuthResult result = authService.refresh(refreshCookie);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.refreshToken()).toString())
                    .body(authService.toResponse(result));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                    .build();
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out", description = "Revokes the refresh token and clears the cookie")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "${app.jwt.refresh-cookie-name:travner_refresh}", required = false) String refreshCookie) {
        authService.logout(refreshCookie);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .build();
    }

    @GetMapping("/me")
    @Operation(summary = "Current user", description = "Returns the authenticated principal's public identity")
    public ResponseEntity<AuthUser> me(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.adnan.travner.security.UserPrincipal userPrincipal) {
            return ResponseEntity.ok(AuthUser.builder()
                    .id(userPrincipal.getId())
                    .username(userPrincipal.getUsername())
                    .email(userPrincipal.getEmail())
                    .roles(userPrincipal.getRoles())
                    .build());
        }
        if (principal instanceof User springUser) {
            // Interim Basic-auth principal during the P1-01 -> P1-02 transition.
            return ResponseEntity.ok(AuthUser.builder()
                    .username(springUser.getUsername())
                    .roles(springUser.getAuthorities().stream()
                            .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                            .toList())
                    .build());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    private ResponseCookie buildRefreshCookie(String rawToken) {
        return ResponseCookie.from(cookieName, rawToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(refreshTokenTtlDays * 24 * 60 * 60)
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(0)
                .build();
    }

    private String clientKey(HttpServletRequest request) {
        return "ip:" + clientAddressResolver.resolveClientIp(request);
    }

    private ResponseEntity<?> rateLimitedResponse(String message, long retryAfterSeconds) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds))
                .body(ApiResponse.error(ErrorCode.RATE_LIMITED.value(), message, traceId()));
    }

    private String traceId() {
        String traceId = MDC.get(CorrelationIdFilter.MDC_KEY);
        return (traceId == null || traceId.isBlank()) ? UUID.randomUUID().toString() : traceId;
    }
}

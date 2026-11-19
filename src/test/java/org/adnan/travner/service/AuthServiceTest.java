package org.adnan.travner.service;

import org.adnan.travner.config.JwtConfig;
import org.adnan.travner.dto.auth.AuthResponse;
import org.adnan.travner.entry.RefreshToken;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.RefreshTokenRepository;
import org.adnan.travner.repository.UserRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;
    private JwtEncoder jwtEncoder;
    private JwtDecoder jwtDecoder;
    private UserEntry user;

    @BeforeEach
    void setUp() throws Exception {
        JwtConfig jwtConfig = new JwtConfig();
        ReflectionTestUtils.setField(jwtConfig, "privateKeyPem", "");
        ReflectionTestUtils.setField(jwtConfig, "publicKeyPem", "");
        ReflectionTestUtils.setField(jwtConfig, "issuer", "https://travner.local");
        var rsaKey = jwtConfig.jwtRsaKey();
        jwtEncoder = jwtConfig.jwtEncoder(jwtConfig.jwtJwkSource(rsaKey));
        jwtDecoder = jwtConfig.jwtDecoder(rsaKey);

        authService = new AuthService(userRepository, refreshTokenRepository,
                passwordEncoder, jwtEncoder);
        ReflectionTestUtils.setField(authService, "accessTokenTtlSeconds", 900L);
        ReflectionTestUtils.setField(authService, "refreshTokenTtlDays", 7L);
        ReflectionTestUtils.setField(authService, "issuer", "https://travner.local");

        user = UserEntry.builder()
                .id(new ObjectId())
                .userName("alice")
                .password("hashed-password")
                .email("alice@example.com")
                .roles(List.of("USER"))
                .active(true)
                .build();
    }

    @Test
    void login_withValidCredentials_issuesSignedTokenAndStoresOnlyHash() {
        when(userRepository.findByuserName("alice")).thenReturn(user);
        when(passwordEncoder.matches("correct", "hashed-password")).thenReturn(true);

        AuthService.AuthResult result = authService.login("alice", "correct");

        assertNotNull(result.accessToken());
        assertNotNull(result.refreshToken());
        Jwt decoded = jwtDecoder.decode(result.accessToken());
        assertEquals(user.getId().toHexString(), decoded.getSubject(), "subject must be the user ID (D2)");
        assertEquals("alice", decoded.getClaimAsString("username"));
        assertEquals("alice@example.com", decoded.getClaimAsString("email"));
        assertEquals(List.of("USER"), decoded.getClaimAsStringList("roles"));
        assertEquals("https://travner.local", decoded.getIssuer().toString());
        assertNotNull(decoded.getExpiresAt());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken stored = captor.getValue();
        assertEquals(hash(result.refreshToken()), stored.getTokenHash());
        assertNotEquals(result.refreshToken(), stored.getTokenHash(), "raw refresh token must never be stored");
        assertEquals(user.getId().toHexString(), stored.getUserId());
        assertNotNull(stored.getExpiresAt());
        assertNull(stored.getRevokedAt());
        verify(userRepository).save(user);
        assertNotNull(user.getLastLoginAt());
    }

    @Test
    void login_withWrongPassword_throwsGenericErrorAndIssuesNoTokens() {
        when(userRepository.findByuserName("alice")).thenReturn(user);
        when(passwordEncoder.matches("wrong", "hashed-password")).thenReturn(false);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.login("alice", "wrong"));

        assertEquals("Invalid username or password", ex.getMessage());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_withUnknownUser_throwsSameGenericError() {
        when(userRepository.findByuserName("nobody")).thenReturn(null);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.login("nobody", "anything"));

        assertEquals("Invalid username or password", ex.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_withInactiveUser_isRejected() {
        user.setActive(false);
        when(userRepository.findByuserName("alice")).thenReturn(user);

        assertThrows(BadCredentialsException.class, () -> authService.login("alice", "correct"));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_rotatesToken_singleUse() {
        String raw = "raw-refresh-token";
        RefreshToken stored = storedToken(raw, Instant.now().plusSeconds(3600), null);
        when(refreshTokenRepository.findByTokenHash(hash(raw))).thenReturn(Optional.of(stored));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        AuthService.AuthResult result = authService.refresh(raw);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(captor.capture());
        List<RefreshToken> saved = captor.getAllValues();
        RefreshToken newToken = saved.stream()
                .filter(t -> t.getRevokedAt() == null).findFirst().orElseThrow();
        RefreshToken oldToken = saved.stream()
                .filter(t -> t.getRevokedAt() != null).findFirst().orElseThrow();

        assertEquals(hash(result.refreshToken()), newToken.getTokenHash());
        assertNotNull(oldToken.getRevokedAt());
        assertEquals(hash(result.refreshToken()), oldToken.getReplacedByHash());
    }

    @Test
    void refresh_withReplayedRevokedToken_revokesAllUserTokens() {
        String raw = "replayed-token";
        RefreshToken stored = storedToken(raw, Instant.now().plusSeconds(3600),
                Instant.now().minusSeconds(30));
        when(refreshTokenRepository.findByTokenHash(hash(raw))).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.findByUserId(user.getId().toHexString())).thenReturn(List.of(stored));

        assertThrows(BadCredentialsException.class, () -> authService.refresh(raw));

        verify(refreshTokenRepository).saveAll(argThat(tokens ->
                java.util.stream.StreamSupport.stream(tokens.spliterator(), false)
                        .allMatch(t -> t.getRevokedAt() != null)));
    }

    @Test
    void refresh_withExpiredToken_isRejected() {
        String raw = "expired-token";
        RefreshToken stored = storedToken(raw, Instant.now().minusSeconds(1), null);
        when(refreshTokenRepository.findByTokenHash(hash(raw))).thenReturn(Optional.of(stored));

        assertThrows(BadCredentialsException.class, () -> authService.refresh(raw));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void expiredAccessToken_isRejectedByDecoder() {
        var expired = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
                .issuer("https://travner.local")
                .issuedAt(Instant.now().minusSeconds(3600))
                .expiresAt(Instant.now().minusSeconds(1800))
                .subject(user.getId().toHexString())
                .build();
        String token = jwtEncoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(
                        org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256).build(),
                expired)).getTokenValue();

        assertThrows(org.springframework.security.oauth2.jwt.JwtValidationException.class,
                () -> jwtDecoder.decode(token));
    }

    @Test
    void logout_revokesMatchingToken() {
        String raw = "logout-token";
        RefreshToken stored = storedToken(raw, Instant.now().plusSeconds(3600), null);
        when(refreshTokenRepository.findByTokenHash(hash(raw))).thenReturn(Optional.of(stored));

        authService.logout(raw);

        assertNotNull(stored.getRevokedAt());
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void logout_withoutCookie_isNoOp() {
        authService.logout(null);
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void toResponse_excludesRawRefreshToken() {
        AuthService.AuthResult result = new AuthService.AuthResult("access", "raw-refresh-secret", user);

        AuthResponse response = authService.toResponse(result);

        assertEquals("access", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(900L, response.getExpiresIn());
        assertEquals(user.getId().toHexString(), response.getUser().getId());
        assertEquals("alice", response.getUser().getUsername());
        assertEquals(List.of("USER"), response.getUser().getRoles());
        assertFalse(response.toString().contains("raw-refresh-secret"));
    }

    private RefreshToken storedToken(String raw, Instant expiresAt, Instant revokedAt) {
        return RefreshToken.builder()
                .id(new ObjectId())
                .tokenHash(hash(raw))
                .userId(user.getId().toHexString())
                .createdAt(Instant.now().minusSeconds(60))
                .expiresAt(expiresAt)
                .revokedAt(revokedAt)
                .build();
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

package org.adnan.travner.service;

import lombok.RequiredArgsConstructor;
import org.adnan.travner.dto.auth.AuthResponse;
import org.adnan.travner.dto.auth.AuthUser;
import org.adnan.travner.entry.RefreshToken;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.RefreshTokenRepository;
import org.adnan.travner.repository.UserRepository;
import org.adnan.travner.util.IdentityNormalizer;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Issues and rotates access/refresh tokens. The raw refresh token is returned
 * to the caller once (as an HttpOnly cookie); only its SHA-256 digest is stored.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String KEY_ID = "travner-access-key";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    @Value("${app.jwt.access-token-ttl-seconds:900}")
    private long accessTokenTtlSeconds;

    @Value("${app.jwt.refresh-token-ttl-days:7}")
    private long refreshTokenTtlDays;

    @Value("${app.jwt.issuer:travner}")
    private String issuer;

    /** Authenticates credentials and issues a new token pair. */
    public AuthResult login(String username, String password) {
        UserEntry user = username == null ? null
                : userRepository.findByuserName(IdentityNormalizer.normalizeUsername(username));
        if (user == null || !user.isActive() || password == null
                || !passwordEncoder.matches(password, user.getPassword())) {
            // Deliberately generic: do not reveal whether the username exists.
            throw new BadCredentialsException("Invalid username or password");
        }
        recordSuccessfulLogin(user);
        return issueTokens(user);
    }

    /** Validates and rotates a refresh token. Single use; replay revokes the user's tokens. */
    public AuthResult refresh(String rawToken) {
        RefreshToken stored = findByRawToken(rawToken)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (stored.getRevokedAt() != null) {
            // A revoked token being replayed indicates theft or reuse. Contain it.
            revokeAllForUser(stored.getUserId());
            throw new BadCredentialsException("Invalid refresh token");
        }
        if (stored.getExpiresAt() == null || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        if (stored.getUserId() == null || !ObjectId.isValid(stored.getUserId())) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        UserEntry user = userRepository.findById(new ObjectId(stored.getUserId())).orElse(null);
        if (user == null || !user.isActive()) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        AuthResult fresh = issueTokens(user);
        stored.setRevokedAt(Instant.now());
        stored.setReplacedByHash(hash(fresh.refreshToken()));
        refreshTokenRepository.save(stored);
        return fresh;
    }

    /** Revokes one refresh token; a missing/blank token is a no-op. */
    public void logout(String rawToken) {
        findByRawToken(rawToken).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            }
        });
    }

    /** Revokes every refresh token for a user (call on password change/reset). */
    public void revokeAllForUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        List<RefreshToken> tokens = refreshTokenRepository.findByUserId(userId);
        tokens.forEach(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
            }
        });
        refreshTokenRepository.saveAll(tokens);
    }

    /** Builds the response body; the raw refresh token is deliberately excluded. */
    public AuthResponse toResponse(AuthResult result) {
        UserEntry user = result.user();
        return AuthResponse.builder()
                .accessToken(result.accessToken())
                .tokenType("Bearer")
                .expiresIn(accessTokenTtlSeconds)
                .user(AuthUser.builder()
                        .id(user.getId().toHexString())
                        .username(user.getUserName())
                        .email(user.getEmail())
                        .roles(user.getRoles())
                        .build())
                .build();
    }

    private AuthResult issueTokens(UserEntry user) {
        Instant now = Instant.now();
        String userId = user.getId().toHexString();
        List<String> roles = user.getRoles() == null || user.getRoles().isEmpty()
                ? List.of("USER") : List.copyOf(user.getRoles());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(accessTokenTtlSeconds))
                .subject(userId)
                .claim("username", user.getUserName())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .build();

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).keyId(KEY_ID).build(),
                claims)).getTokenValue();

        String rawRefresh = generateRefreshToken();
        RefreshToken entity = RefreshToken.builder()
                .tokenHash(hash(rawRefresh))
                .userId(userId)
                .createdAt(now)
                .expiresAt(now.plusSeconds(refreshTokenTtlDays * 24 * 60 * 60))
                .build();
        refreshTokenRepository.save(entity);

        return new AuthResult(accessToken, rawRefresh, user);
    }

    private Optional<RefreshToken> findByRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return refreshTokenRepository.findByTokenHash(hash(rawToken));
    }

    private void recordSuccessfulLogin(UserEntry user) {
        try {
            user.setLastLoginAt(java.time.LocalDateTime.now());
            userRepository.save(user);
        } catch (Exception ignored) {
            // Bookkeeping must never fail an otherwise valid login.
        }
    }

    private static String generateRefreshToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the JDK", e);
        }
    }

    /** Issued access token, one-time raw refresh token, and the authenticated user. */
    public record AuthResult(String accessToken, String refreshToken, UserEntry user) {
    }
}

package org.adnan.travner.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtAuthenticationConverterNormalizationTest {

    @Test
    void legacyMixedCaseClaimsAreCanonicalizedForPrincipalAndName() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("6b0042d7f97c6792ad57c7de")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .claim("username", "  Alice_01  ".trim())
                .claim("email", " ALICE@Example.COM ")
                .claim("roles", List.of("USER"))
                .build();

        Authentication authentication = new JwtAuthenticationConverter().convert(jwt);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        assertEquals("alice_01", principal.getUsername());
        assertEquals("alice_01", authentication.getName());
        assertEquals("alice@example.com", principal.getEmail());
        assertEquals("6b0042d7f97c6792ad57c7de", principal.getId());
    }

    @Test
    void missingUsernameClaimDoesNotThrow() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("6b0042d7f97c6792ad57c7de")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .claims(claims -> claims.putAll(Map.of("roles", List.of("USER"))))
                .build();

        Authentication authentication = new JwtAuthenticationConverter().convert(jwt);

        assertEquals(null, ((UserPrincipal) authentication.getPrincipal()).getUsername());
    }
}

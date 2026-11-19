package org.adnan.travner.security;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.List;

/**
 * Signed access tokens shared by the auth/authz matrix tests. All tokens use
 * the real {@link JwtEncoder} from {@code JwtConfig}, so signature and issuer
 * validation are exercised instead of bypassed with mocks.
 */
public final class JwtTestTokenFactory {

    public static final String ISSUER = "https://travner.local";
    public static final String USER_A_ID = "507f1f77bcf86cd799439011";
    public static final String USER_B_ID = "507f191e810c19729de860ea";
    public static final String ADMIN_ID = "507f1f77bcf86cd799439012";

    public static final String USER_A = "alice";
    public static final String USER_B = "bob";
    public static final String ADMIN = "admin";

    private JwtTestTokenFactory() {
    }

    /** A valid token for a user with the supplied role names (e.g. USER, ADMIN). */
    public static String token(JwtEncoder encoder, String subject, String username, String... roles) {
        return sign(encoder, ISSUER, subject, username, List.of(roles), Instant.now().plusSeconds(900));
    }

    public static String expired(JwtEncoder encoder, String subject, String username) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now.minusSeconds(900))
                .expiresAt(now.minusSeconds(60))
                .subject(subject)
                .claim("username", username)
                .claim("roles", List.of("USER"))
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
    }

    public static String wrongIssuer(JwtEncoder encoder, String subject, String username) {
        return sign(encoder, "https://attacker.example", subject, username,
                List.of("USER"), Instant.now().plusSeconds(900));
    }

    /** A correctly signed token with no roles claim: authenticated but not authorized. */
    public static String withoutRoles(JwtEncoder encoder, String subject, String username) {
        return sign(encoder, ISSUER, subject, username, List.of(), Instant.now().plusSeconds(900));
    }

    private static String sign(JwtEncoder encoder, String issuer, String subject, String username,
                               List<String> roles, Instant expiresAt) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(subject)
                .claim("username", username)
                .claim("roles", roles)
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
    }
}

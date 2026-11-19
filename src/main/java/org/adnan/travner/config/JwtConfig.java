package org.adnan.travner.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Signing keys and JWT encoder/decoder for access tokens.
 *
 * <p>Keys can be supplied as PKCS#8/X.509 PEM through {@code JWT_PRIVATE_KEY_PEM}
 * and {@code JWT_PUBLIC_KEY_PEM}. If absent, an ephemeral RSA-2048 pair is
 * generated at startup — access tokens are short-lived, so a restart simply
 * invalidates outstanding access tokens (clients refresh automatically).</p>
 */
@Configuration
public class JwtConfig {

    @Value("${JWT_PRIVATE_KEY_PEM:}")
    private String privateKeyPem;

    @Value("${JWT_PUBLIC_KEY_PEM:}")
    private String publicKeyPem;

    @Value("${app.jwt.issuer:travner}")
    private String issuer;

    @Bean
    public RSAKey jwtRsaKey() throws Exception {
        RSAPrivateKey privateKey;
        RSAPublicKey publicKey;

        if (privateKeyPem != null && !privateKeyPem.isBlank()) {
            privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(
                    new PKCS8EncodedKeySpec(decodePem(privateKeyPem, "PRIVATE KEY")));
            if (publicKeyPem != null && !publicKeyPem.isBlank()) {
                publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
                        new X509EncodedKeySpec(decodePem(publicKeyPem, "PUBLIC KEY")));
            } else if (privateKey instanceof RSAPrivateCrtKey crtKey) {
                publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
                        new RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent()));
            } else {
                throw new IllegalStateException("JWT_PUBLIC_KEY_PEM is required for a non-CRT private key");
            }
        } else {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            privateKey = (RSAPrivateKey) pair.getPrivate();
            publicKey = (RSAPublicKey) pair.getPublic();
        }

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("travner-access-key")
                .build();
    }

    @Bean
    public JWKSource<SecurityContext> jwtJwkSource(RSAKey rsaKey) {
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (JWKSelector selector, SecurityContext context) -> selector.select(jwkSet);
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaKey) throws JOSEException {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        // Signature plus exp/nbf are the defaults; the issuer must also match or a
        // token minted for another audience is rejected (P1-06 invalid-token matrix).
        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(issuer);
        decoder.setJwtValidator(validator);
        return decoder;
    }

    private static byte[] decodePem(String pem, String label) {
        String body = pem
                .replace("-----BEGIN " + label + "-----", "")
                .replace("-----END " + label + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }
}

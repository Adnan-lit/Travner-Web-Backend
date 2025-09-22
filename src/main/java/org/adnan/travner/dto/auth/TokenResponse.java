package org.adnan.travner.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for JWT token generation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResponse {
    
    /**
     * JWT access token for WebSocket authentication
     */
    private String accessToken;
    
    /**
     * Token type (always "Bearer")
     */
    @Builder.Default
    private String tokenType = "Bearer";
    
    /**
     * Token expiration time in milliseconds
     */
    private long expiresIn;
    
    /**
     * Token issue timestamp
     */
    private Instant issuedAt;
    
    /**
     * Username for which the token was issued
     */
    private String username;
}
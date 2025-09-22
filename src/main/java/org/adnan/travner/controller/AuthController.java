package org.adnan.travner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.auth.TokenResponse;
import org.adnan.travner.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Authentication controller for JWT token management
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and token management APIs")
public class AuthController {

    private final JwtService jwtService;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    /**
     * Generate JWT token for authenticated user (for WebSocket connections)
     */
    @PostMapping("/token")
    @Operation(
        summary = "Generate JWT Token", 
        description = "Generate a JWT token for WebSocket authentication. Requires Basic Authentication."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token generated successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<org.adnan.travner.dto.ApiResponse<TokenResponse>> generateToken(
            Authentication authentication) {
        
        log.debug("Generating JWT token for user: {}", authentication.getName());
        
        String username = authentication.getName();
        String jwtToken = jwtService.generateToken(username);
        
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(jwtToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration)
                .issuedAt(Instant.now())
                .username(username)
                .build();
        
        org.adnan.travner.dto.ApiResponse<TokenResponse> response = 
            org.adnan.travner.dto.ApiResponse.<TokenResponse>builder()
                .success(true)
                .message("JWT token generated successfully")
                .data(tokenResponse)
                .build();
        
        log.info("JWT token generated for user: {}", username);
        return ResponseEntity.ok(response);
    }

    /**
     * Validate JWT token
     */
    @PostMapping("/validate")
    @Operation(
        summary = "Validate JWT Token", 
        description = "Validate a JWT token and return user information"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token is valid"),
        @ApiResponse(responseCode = "401", description = "Token is invalid or expired")
    })
    public ResponseEntity<org.adnan.travner.dto.ApiResponse<String>> validateToken(
            @RequestParam String token) {
        
        log.debug("Validating JWT token");
        
        try {
            if (jwtService.isTokenValid(token)) {
                String username = jwtService.extractUsername(token);
                
                org.adnan.travner.dto.ApiResponse<String> response = 
                    org.adnan.travner.dto.ApiResponse.<String>builder()
                        .success(true)
                        .message("Token is valid")
                        .data(username)
                        .build();
                
                return ResponseEntity.ok(response);
            } else {
                org.adnan.travner.dto.ApiResponse<String> response = 
                    org.adnan.travner.dto.ApiResponse.<String>builder()
                        .success(false)
                        .message("Token is invalid or expired")
                        .data(null)
                        .build();
                
                return ResponseEntity.status(401).body(response);
            }
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            
            org.adnan.travner.dto.ApiResponse<String> response = 
                org.adnan.travner.dto.ApiResponse.<String>builder()
                    .success(false)
                    .message("Token validation failed: " + e.getMessage())
                    .data(null)
                    .build();
            
            return ResponseEntity.status(401).body(response);
        }
    }
}
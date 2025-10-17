package org.adnan.travner.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Rate limiting filter to prevent abuse of API endpoints
 */
@Component
@Order(1)
@Slf4j
public class RateLimitingFilter implements Filter {

    @Autowired
    private RateLimitingConfig rateLimitingConfig;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip rate limiting for health checks and static resources
        String requestPath = httpRequest.getRequestURI();
        if (isExcludedPath(requestPath)) {
            chain.doFilter(request, response);
            return;
        }

        // Get client identifier (IP address or user ID if authenticated)
        String clientId = getClientIdentifier(httpRequest);
        
        // Apply rate limiting based on endpoint type
        boolean allowed = isRateLimitAllowed(httpRequest, clientId);
        
        if (allowed) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for client: {} on path: {}", clientId, requestPath);
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"success\":false,\"message\":\"Rate limit exceeded. Please try again later.\"}");
        }
    }

    private boolean isExcludedPath(String path) {
        return path.startsWith("/actuator/health") ||
               path.startsWith("/actuator/info") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/error");
    }

    private String getClientIdentifier(HttpServletRequest request) {
        // Try to get authenticated user first
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            // For authenticated users, use username as identifier
            try {
                String credentials = new String(java.util.Base64.getDecoder().decode(authHeader.substring(6)));
                String username = credentials.split(":")[0];
                return "user:" + username;
            } catch (Exception e) {
                // Fall back to IP if parsing fails
            }
        }
        
        // Fall back to IP address for anonymous users
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return "ip:" + xForwardedFor.split(",")[0].trim();
        }
        
        return "ip:" + request.getRemoteAddr();
    }

    private boolean isRateLimitAllowed(HttpServletRequest request, String clientId) {
        String path = request.getRequestURI();
        
        // Different rate limits for different endpoint types
        if (path.startsWith("/api/public/register")) {
            return rateLimitingConfig.isLoginAllowed(clientId);
        } else if (path.startsWith("/api/media/upload")) {
            return rateLimitingConfig.isUploadAllowed(clientId);
        } else if (path.startsWith("/api/chat/") || path.startsWith("/ws/")) {
            return rateLimitingConfig.isChatAllowed(clientId);
        } else {
            return rateLimitingConfig.isApiAllowed(clientId);
        }
    }
}

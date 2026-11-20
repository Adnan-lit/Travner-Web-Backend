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
    private RateLimiter rateLimiter;

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
            log.warn("Rate limit exceeded [trace={}] for client: {} on path: {}",
                    org.slf4j.MDC.get(CorrelationIdFilter.MDC_KEY), clientId, requestPath);
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.setContentType("application/json");
            String traceId = org.slf4j.MDC.get(CorrelationIdFilter.MDC_KEY);
            if (traceId == null || traceId.isBlank()) {
                traceId = java.util.UUID.randomUUID().toString();
            }
            httpResponse.setHeader(CorrelationIdFilter.HEADER, traceId);
            httpResponse.getWriter().write(
                    "{\"success\":false,\"code\":\"RATE_LIMITED\","
                            + "\"message\":\"Rate limit exceeded. Please try again later.\","
                            + "\"traceId\":\"" + traceId + "\"}");
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
        // Authentication happens after this filter, so the SecurityContext is
        // not populated yet. Never decode credentials here (Basic was removed).
        // Key on the client IP; trust X-Forwarded-For only behind a known proxy.
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
            return rateLimiter.isLoginAllowed(clientId);
        } else if (path.startsWith("/api/media/upload") || path.startsWith("/api/chat/upload")) {
            return rateLimiter.isUploadAllowed(clientId);
        } else if (path.startsWith("/api/chat/") || path.startsWith("/ws/")) {
            return rateLimiter.isChatAllowed(clientId);
        } else {
            return rateLimiter.isApiAllowed(clientId);
        }
    }
}

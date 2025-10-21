package org.adnan.travner.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple rate limiting configuration using in-memory storage
 * Provides rate limiting for API endpoints and chat functionality
 */
@Configuration
@Component
public class RateLimitingConfig {

    // Simple rate limiting using timestamps
    private final Map<String, RateLimitEntry> rateLimitStore = new ConcurrentHashMap<>();

    /**
     * Check if request is within rate limit
     * Thread-safe implementation using synchronized blocks
     */
    public boolean isAllowed(String key, int maxRequests, int windowMinutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusMinutes(windowMinutes);
        
        RateLimitEntry entry = rateLimitStore.computeIfAbsent(key, k -> new RateLimitEntry());
        
        // Synchronize access to the entry's request list
        synchronized (entry) {
            // Remove old requests outside the window
            entry.requests.removeIf(requestTime -> requestTime.isBefore(windowStart));
            
            // Check if under limit
            if (entry.requests.size() < maxRequests) {
                entry.requests.add(now);
                return true;
            }
            
            return false;
        }
    }

    /**
     * Get API rate limiting (100 requests per minute)
     */
    public boolean isApiAllowed(String key) {
        return isAllowed("api:" + key, 100, 1);
    }

    /**
     * Get chat rate limiting (60 messages per minute)
     */
    public boolean isChatAllowed(String key) {
        return isAllowed("chat:" + key, 60, 1);
    }

    /**
     * Get login rate limiting (5 attempts per minute)
     */
    public boolean isLoginAllowed(String key) {
        return isAllowed("login:" + key, 5, 1);
    }

    /**
     * Get upload rate limiting (10 uploads per hour)
     */
    public boolean isUploadAllowed(String key) {
        return isAllowed("upload:" + key, 10, 60);
    }

    /**
     * Simple rate limit entry
     */
    private static class RateLimitEntry {
        private final java.util.List<LocalDateTime> requests = new java.util.ArrayList<>();
    }
}

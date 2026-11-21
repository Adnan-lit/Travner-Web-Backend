package org.adnan.travner.config;

/**
 * Abstraction for rate limiting so callers do not depend on the in-memory
 * implementation and web slices can substitute a mock.
 */
public interface RateLimiter {

    boolean isAllowed(String key, int maxRequests, int windowMinutes);

    RateLimitDecision allow(String key, int maxRequests, int windowMinutes);

    boolean isApiAllowed(String key);

    RateLimitDecision allowApi(String key);

    boolean isChatAllowed(String key);

    RateLimitDecision allowChat(String key);

    boolean isLoginAllowed(String key);

    RateLimitDecision allowLogin(String key);

    RateLimitDecision allowRefresh(String key);

    boolean isUploadAllowed(String key);

    RateLimitDecision allowUpload(String key);
}

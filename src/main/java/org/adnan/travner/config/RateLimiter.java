package org.adnan.travner.config;

/**
 * Abstraction for rate limiting so callers do not depend on the in-memory
 * implementation and web slices can substitute a mock.
 */
public interface RateLimiter {

    boolean isAllowed(String key, int maxRequests, int windowMinutes);

    boolean isApiAllowed(String key);

    boolean isChatAllowed(String key);

    boolean isLoginAllowed(String key);

    boolean isUploadAllowed(String key);
}

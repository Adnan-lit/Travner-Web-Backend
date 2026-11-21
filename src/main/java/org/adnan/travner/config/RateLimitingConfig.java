package org.adnan.travner.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * In-memory rate limiting with TTL eviction. Keys expire automatically so the
 * limiter remains bounded in local and single-node deployments.
 */
@Component
public class RateLimitingConfig implements RateLimiter {

    private final Cache<String, RateLimitEntry> rateLimitStore = Caffeine.newBuilder()
            .expireAfter(new RateLimitExpiry())
            .maximumSize(20_000)
            .build();

    /**
     * Check if request is within rate limit
     * Thread-safe implementation using synchronized blocks
     */
    @Override
    public boolean isAllowed(String key, int maxRequests, int windowMinutes) {
        return allow(key, maxRequests, windowMinutes).allowed();
    }

    @Override
    public RateLimitDecision allow(String key, int maxRequests, int windowMinutes) {
        Instant now = Instant.now();
        Duration window = Duration.ofMinutes(windowMinutes);
        Instant windowStart = now.minus(window);

        RateLimitEntry entry = rateLimitStore.get(key, unused -> new RateLimitEntry());
        synchronized (entry) {
            entry.requests.removeIf(requestTime -> requestTime.isBefore(windowStart));

            if (entry.requests.size() < maxRequests) {
                entry.requests.add(now);
                entry.nextExpiryAt = now.plus(window);
                return RateLimitDecision.permit();
            }

            Instant oldestRetainedRequest = entry.requests.get(0);
            long retryAfterSeconds = Math.max(1, Duration.between(now, oldestRetainedRequest.plus(window)).toSeconds());
            entry.nextExpiryAt = oldestRetainedRequest.plus(window);
            return RateLimitDecision.rejected(retryAfterSeconds);
        }
    }

    /**
     * Get API rate limiting (100 requests per minute)
     */
    @Override
    public boolean isApiAllowed(String key) {
        return allowApi(key).allowed();
    }

    @Override
    public RateLimitDecision allowApi(String key) {
        return allow("api:" + key, 100, 1);
    }

    /**
     * Get chat rate limiting (60 messages per minute)
     */
    @Override
    public boolean isChatAllowed(String key) {
        return allowChat(key).allowed();
    }

    @Override
    public RateLimitDecision allowChat(String key) {
        return allow("chat:" + key, 60, 1);
    }

    /**
     * Get login rate limiting (5 attempts per minute)
     */
    @Override
    public boolean isLoginAllowed(String key) {
        return allowLogin(key).allowed();
    }

    @Override
    public RateLimitDecision allowLogin(String key) {
        return allow("login:" + key, 5, 1);
    }

    @Override
    public RateLimitDecision allowRefresh(String key) {
        return allow("refresh:" + key, 10, 1);
    }

    /**
     * Get upload rate limiting (10 uploads per hour)
     */
    @Override
    public boolean isUploadAllowed(String key) {
        return allowUpload(key).allowed();
    }

    @Override
    public RateLimitDecision allowUpload(String key) {
        return allow("upload:" + key, 10, 60);
    }

    /**
     * Simple rate limit entry
     */
    private static class RateLimitEntry {
        private final java.util.List<Instant> requests = new java.util.ArrayList<>();
        private Instant nextExpiryAt = Instant.now().plus(Duration.ofMinutes(1));
    }

    private static final class RateLimitExpiry implements com.github.benmanes.caffeine.cache.Expiry<String, RateLimitEntry> {
        @Override
        public long expireAfterCreate(String key, RateLimitEntry value, long currentTime) {
            return nanosUntil(value);
        }

        @Override
        public long expireAfterUpdate(String key, RateLimitEntry value, long currentTime, long currentDuration) {
            return nanosUntil(value);
        }

        @Override
        public long expireAfterRead(String key, RateLimitEntry value, long currentTime, long currentDuration) {
            return currentDuration;
        }

        private long nanosUntil(RateLimitEntry value) {
            long nanos = Duration.between(Instant.now(), value.nextExpiryAt).toNanos();
            return Math.max(TimeUnit.SECONDS.toNanos(1), nanos);
        }
    }
}

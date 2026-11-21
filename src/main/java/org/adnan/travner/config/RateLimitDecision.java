package org.adnan.travner.config;

/** Outcome of one rate-limit check, including retry metadata for 429 responses. */
public record RateLimitDecision(boolean allowed, long retryAfterSeconds) {

    public static RateLimitDecision permit() {
        return new RateLimitDecision(true, 0);
    }

    public static RateLimitDecision rejected(long retryAfterSeconds) {
        return new RateLimitDecision(false, Math.max(1, retryAfterSeconds));
    }
}

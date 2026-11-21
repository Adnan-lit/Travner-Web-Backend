package org.adnan.travner.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingConfigTest {

    private final RateLimitingConfig rateLimiter = new RateLimitingConfig();

    @Test
    void allowApiRejectsRequestWithRetryAfterAfterBurst() {
        for (int i = 0; i < 100; i++) {
            assertThat(rateLimiter.allowApi("ip:198.51.100.10").allowed()).isTrue();
        }

        RateLimitDecision blocked = rateLimiter.allowApi("ip:198.51.100.10");

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.retryAfterSeconds()).isPositive();
    }

    @Test
    void loginAndRefreshUseSeparateBuckets() {
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.allowLogin("ip:198.51.100.11").allowed()).isTrue();
        }

        assertThat(rateLimiter.allowLogin("ip:198.51.100.11").allowed()).isFalse();
        assertThat(rateLimiter.allowRefresh("ip:198.51.100.11").allowed()).isTrue();
    }
}

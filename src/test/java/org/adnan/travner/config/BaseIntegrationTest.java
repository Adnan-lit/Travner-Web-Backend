package org.adnan.travner.config;

import org.springframework.boot.test.context.TestConfiguration;

/**
 * Base test class for integration tests using embedded MongoDB
 */
@TestConfiguration
public class BaseIntegrationTest {
    // MongoDB will be auto-configured by Spring Boot with embedded Flapdoodle
    // when de.flapdoodle.embed.mongo is on the classpath
}

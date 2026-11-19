package org.adnan.travner.config;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests that need MongoDB with transaction support.
 *
 * <p>The container starts a single-node replica set, which is required by
 * {@code @Transactional}. Tests are skipped automatically when Docker is not
 * available (for example on a developer machine); CI runs them with Docker.</p>
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class BaseIntegrationTest {

    @Container
    static final MongoDBContainer MONGODB = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGODB::getReplicaSetUrl);
    }
}

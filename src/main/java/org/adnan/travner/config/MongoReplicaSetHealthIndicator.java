package org.adnan.travner.config;

import org.bson.Document;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Surfaces whether MongoDB is a replica set, which Spring {@code @Transactional}
 * requires. This indicator stays UP when the server is standalone: the public
 * uptime probe must not change status, but the {@code replicaSet} detail and the
 * startup warning make the misconfiguration visible.
 */
@Component("mongoReplicaSet")
public class MongoReplicaSetHealthIndicator implements HealthIndicator {

    private final MongoTemplate mongoTemplate;

    public MongoReplicaSetHealthIndicator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Health health() {
        try {
            Document hello = mongoTemplate.getDb().runCommand(new Document("hello", 1));
            boolean replicaSet = hello != null && hello.getString("setName") != null;
            return Health.up().withDetail("replicaSet", replicaSet).build();
        } catch (Exception e) {
            return Health.up()
                    .withDetail("replicaSet", false)
                    .withDetail("probe", "unavailable")
                    .build();
        }
    }
}

package org.adnan.travner.config;

import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MongoReplicaSetHealthIndicatorTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private MongoDatabase database;

    private MongoReplicaSetHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new MongoReplicaSetHealthIndicator(mongoTemplate);
        when(mongoTemplate.getDb()).thenReturn(database);
    }

    @Test
    void replicaSetServerIsReportedAsUpWithTrueDetail() {
        when(database.runCommand(any(Document.class)))
                .thenReturn(new Document("setName", "rs0"));

        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(true, health.getDetails().get("replicaSet"));
    }

    @Test
    void standaloneServerIsStillUpButReportsFalseDetail() {
        when(database.runCommand(any(Document.class))).thenReturn(new Document("ok", 1));

        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(false, health.getDetails().get("replicaSet"));
    }

    @Test
    void failedProbeDoesNotBreakThePublicUptimeCheck() {
        when(database.runCommand(any(Document.class)))
                .thenThrow(new RuntimeException("probe failed"));

        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(false, health.getDetails().get("replicaSet"));
        assertEquals("unavailable", health.getDetails().get("probe"));
    }
}

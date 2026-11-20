package org.adnan.travner.config;

import org.adnan.travner.TravnerApplication;
import org.adnan.travner.entry.CartEntry;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = TravnerApplication.class)
@ActiveProfiles("test")
class DatabaseIndexInitializerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private DatabaseIndexInitializer initializer;

    @BeforeEach
    void cleanCollections() {
        List.of("users", "carts", "follows", "post_votes")
                .forEach(mongoTemplate::dropCollection);
    }

    @Test
    void normalizesLegacyRowsAndCreatesRealUniqueIndexes() {
        mongoTemplate.insert(user(" Alice ", " ALICE@Example.COM "), "users");
        mongoTemplate.insert(user("Bob", "bob@example.com"), "users");
        mongoTemplate.insert(CartEntry.builder().id(new ObjectId()).userId(" Alice ").build());

        initializer.run(null);

        List<Document> users = mongoTemplate.findAll(Document.class, "users");
        assertEquals(Set.of("alice", "bob"), users.stream()
                .map(user -> user.getString("userName")).collect(Collectors.toSet()));
        assertEquals(Set.of("alice@example.com", "bob@example.com"), users.stream()
                .map(user -> user.getString("email")).collect(Collectors.toSet()));
        // Read the stored document, not the entity: its setter would hide a
        // failed migration by normalizing the value again on load.
        assertEquals("alice", mongoTemplate.findAll(Document.class, "carts").get(0).getString("userId"));

        Set<String> userIndexes = mongoTemplate.indexOps("users").getIndexInfo().stream()
                .filter(IndexInfo::isUnique)
                .map(IndexInfo::getName)
                .collect(Collectors.toSet());
        assertTrue(userIndexes.containsAll(Set.of("users_userName_unique", "users_email_unique")));
        assertTrue(uniqueNames("post_votes").contains("post_votes_post_user_unique"));
        assertTrue(uniqueNames("follows").contains("follows_follower_following_unique"));
        assertTrue(uniqueNames("carts").contains("carts_userId_unique"));
    }

    @Test
    void duplicateNormalizedUsernamesAbortTheMigration() {
        mongoTemplate.insert(user("Alice", "alice@example.com"), "users");
        mongoTemplate.insert(user("alice", "other@example.com"), "users");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> initializer.run(null));

        assertTrue(ex.getMessage().contains("username"));
    }

    private Set<String> uniqueNames(String collection) {
        return mongoTemplate.indexOps(collection).getIndexInfo().stream()
                .filter(IndexInfo::isUnique)
                .map(IndexInfo::getName)
                .collect(Collectors.toSet());
    }

    private Document user(String userName, String email) {
        Document user = new Document("_id", new ObjectId());
        user.put("userName", userName);
        user.put("email", email);
        return user;
    }
}

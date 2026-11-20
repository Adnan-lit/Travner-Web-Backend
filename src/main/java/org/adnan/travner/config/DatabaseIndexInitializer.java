package org.adnan.travner.config;

import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.entry.CartEntry;
import org.adnan.travner.entry.FollowEntry;
import org.adnan.travner.entry.PostVoteEntry;
import org.adnan.travner.util.IdentityNormalizer;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import com.mongodb.client.MongoDatabase;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexField;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds the uniqueness guarantees that P2-06 requires.
 *
 * <p>{@code auto-index-creation} is deliberately disabled, so annotations on
 * entities are documentation only. This runner normalizes legacy identity
 * values from raw Mongo documents, removes safe duplicate relationship rows,
 * aborts startup on ambiguous user duplicates, and then creates the unique
 * indexes. P2-07 can extend the same runner with non-unique indexes.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class DatabaseIndexInitializer implements ApplicationRunner {

    private static final String USERS = "users";
    private static final String CARTS = "carts";
    private static final String FOLLOWS = "follows";
    private static final String POST_VOTES = "post_votes";
    private static final String POSTS = "posts";
    private static final String PRODUCTS = "products";
    private static final String CHAT_MESSAGES = "chat_messages";
    private static final String CHAT_CONVERSATIONS = "chat_conversations";
    private static final String ORDERS = "orders";

    private final MongoTemplate mongoTemplate;

    public DatabaseIndexInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        warnIfNotReplicaSet();
        normalizeUsers();
        normalizeCarts();
        deduplicateFollows();
        deduplicateVotes();
        createUniqueIndexes();
        createCompoundIndexes();
        log.info("Identity normalization and index verification complete");
    }

    /** Standalone MongoDB cannot run {@code @Transactional} writes; warn, do not abort. */
    private void warnIfNotReplicaSet() {
        try {
            MongoDatabase database = mongoTemplate.getDb();
            Document hello = database == null ? null : database.runCommand(new Document("hello", 1));
            if (hello == null || hello.getString("setName") == null) {
                log.warn("MongoDB is not a replica set. @Transactional writes (checkout, cart, follow, "
                        + "post, order) require a replica set; see DEPLOYMENT.md. "
                        + "This warning does not stop startup.");
            }
        } catch (Exception e) {
            log.debug("Could not determine MongoDB replica-set status", e);
        }
    }

    /** Raw documents avoid entity setters, so we can detect what is actually stored. */
    private void normalizeUsers() {
        List<Document> users = mongoTemplate.findAll(Document.class, USERS);
        long missingUsername = users.stream()
                .filter(user -> isBlank(user.getString("userName"))).count();
        long missingEmail = users.stream()
                .filter(user -> isBlank(user.getString("email"))).count();
        if (missingUsername > 0 || missingEmail > 0) {
            throw new IllegalStateException("Cannot build unique user indexes: "
                    + missingUsername + " user(s) without a username, "
                    + missingEmail + " user(s) without an email");
        }

        Map<String, Long> usernameCounts = users.stream()
                .map(user -> IdentityNormalizer.normalizeUsername(user.getString("userName")))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        Map<String, Long> emailCounts = users.stream()
                .map(user -> IdentityNormalizer.normalizeEmail(user.getString("email")))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        failOnDuplicates(usernameCounts, "username");
        failOnDuplicates(emailCounts, "email");

        int rewritten = 0;
        for (Document user : users) {
            String username = IdentityNormalizer.normalizeUsername(user.getString("userName"));
            String email = IdentityNormalizer.normalizeEmail(user.getString("email"));
            boolean changed = false;
            if (!Objects.equals(username, user.getString("userName"))) {
                user.put("userName", username);
                changed = true;
            }
            if (!Objects.equals(email, user.getString("email"))) {
                user.put("email", email);
                changed = true;
            }
            if (changed) {
                mongoTemplate.save(user, USERS);
                rewritten++;
            }
        }
        if (rewritten > 0) {
            log.info("Normalized identity fields for {} existing user(s)", rewritten);
        }
    }

    /** Duplicate carts from case variants are merged, never silently dropped. */
    private void normalizeCarts() {
        // Read raw documents first: the CartEntry setter canonicalizes on load,
        // which would otherwise hide legacy mixed-case keys from this migration.
        List<Document> rawCarts = mongoTemplate.findAll(Document.class, CARTS);
        long missingUserId = rawCarts.stream()
                .filter(cart -> isBlank(cart.getString("userId"))).count();
        if (missingUserId > 0) {
            throw new IllegalStateException("Cannot build the unique cart index: "
                    + missingUserId + " cart(s) without a userId");
        }
        Set<ObjectId> needsRewrite = rawCarts.stream()
                .filter(cart -> !Objects.equals(
                        IdentityNormalizer.normalizeUsername(cart.getString("userId")),
                        cart.getString("userId")))
                .map(cart -> cart.get("_id", ObjectId.class))
                .collect(Collectors.toSet());

        List<CartEntry> carts = mongoTemplate.findAll(CartEntry.class);
        Map<String, List<CartEntry>> byUser = carts.stream().collect(Collectors.groupingBy(
                cart -> IdentityNormalizer.normalizeUsername(cart.getUserId())));

        for (Map.Entry<String, List<CartEntry>> entry : byUser.entrySet()) {
            String userId = entry.getKey();
            List<CartEntry> group = new ArrayList<>(entry.getValue());
            group.sort(Comparator.comparing(CartEntry::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder())));

            CartEntry keeper = group.get(0);
            boolean keeperChanged = needsRewrite.contains(keeper.getId());
            keeper.setUserId(userId);
            if (keeper.getItems() == null) {
                keeper.setItems(new ArrayList<>());
            }

            for (int i = 1; i < group.size(); i++) {
                CartEntry duplicate = group.get(i);
                if (duplicate.getItems() != null) {
                    duplicate.getItems().stream()
                            .filter(item -> item != null && item.getProductId() != null)
                            .forEach(keeper::addItem);
                }
                mongoTemplate.remove(duplicate);
                keeperChanged = true;
            }
            if (keeperChanged) {
                keeper.calculateTotalAmount();
                mongoTemplate.save(keeper);
            }
        }
    }

    /** Exact duplicate relationships are safe to remove; keep the oldest row. */
    private void deduplicateFollows() {
        List<FollowEntry> follows = mongoTemplate.findAll(FollowEntry.class);
        deduplicate(follows,
                follow -> pairKey(follow.getFollowerId(), follow.getFollowingId()),
                FollowEntry::getCreatedAt,
                "follows");
    }

    private void deduplicateVotes() {
        List<PostVoteEntry> votes = mongoTemplate.findAll(PostVoteEntry.class);
        deduplicate(votes,
                vote -> pairKey(vote.getPostId(), vote.getUserId()),
                PostVoteEntry::getCreatedAt,
                "post_votes");
    }

    private <T> void deduplicate(List<T> entries,
                                 Function<T, String> keyFunction,
                                 Function<T, LocalDateTime> createdAtFunction,
                                 String label) {
        Map<String, List<T>> groups = new LinkedHashMap<>();
        for (T entry : entries) {
            String key = keyFunction.apply(entry);
            if (key == null) {
                throw new IllegalStateException("Cannot build the unique " + label
                        + " index: a row has a null identifier");
            }
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(entry);
        }

        int removed = 0;
        for (List<T> group : groups.values()) {
            if (group.size() < 2) {
                continue;
            }
            group.sort(Comparator.comparing(createdAtFunction,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            for (T duplicate : group.subList(1, group.size())) {
                mongoTemplate.remove(duplicate);
                removed++;
            }
        }
        if (removed > 0) {
            log.warn("Removed {} duplicate {} row(s) before creating the unique index", removed, label);
        }
    }

    private void createUniqueIndexes() {
        ensureUniqueIndex(USERS, "users_userName_unique", "userName");
        ensureUniqueIndex(USERS, "users_email_unique", "email");
        ensureUniqueIndex(FOLLOWS, "follows_follower_following_unique", "followerId", "followingId");
        ensureUniqueIndex(POST_VOTES, "post_votes_post_user_unique", "postId", "userId");
        ensureUniqueIndex(CARTS, "carts_userId_unique", "userId");
    }

    /**
     * Keys use persisted BSON names, not Java property names: {@code author} is a
     * DBRef (so its id is {@code author.$id}), products map to snake_case via
     * {@code @Field}, and the active conversation collection is
     * {@code chat_conversations}. Orders have no root {@code sellerId}; the only
     * seller field is {@code items.sellerId}, so that key is used instead.
     */
    private void createCompoundIndexes() {
        ensureIndex(POSTS, "posts_published_createdAt_idx", false,
                new Index().on("published", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC));
        ensureIndex(POSTS, "posts_author_createdAt_idx", false,
                new Index().on("author.$id", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC));
        ensureIndex(PRODUCTS, "products_available_createdAt_idx", false,
                new Index().on("is_available", Sort.Direction.ASC).on("created_at", Sort.Direction.DESC));
        ensureIndex(PRODUCTS, "products_category_available_idx", false,
                new Index().on("category", Sort.Direction.ASC).on("is_available", Sort.Direction.ASC));
        ensureIndex(CHAT_MESSAGES, "chat_messages_conversation_createdAt_idx", false,
                new Index().on("conversationId", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC));
        ensureIndex(CHAT_CONVERSATIONS, "chat_conversations_participants_lastMessageAt_idx", false,
                new Index().on("participantIds", Sort.Direction.ASC).on("lastMessageAt", Sort.Direction.DESC));
        ensureIndex(ORDERS, "orders_userId_orderedAt_idx", false,
                new Index().on("userId", Sort.Direction.ASC).on("orderedAt", Sort.Direction.DESC));
        ensureIndex(ORDERS, "orders_itemsSellerId_status_idx", false,
                new Index().on("items.sellerId", Sort.Direction.ASC).on("status", Sort.Direction.ASC));
    }

    private void ensureUniqueIndex(String collection, String name, String... fields) {
        Index index = new Index();
        for (String field : fields) {
            index.on(field, Sort.Direction.ASC);
        }
        ensureIndex(collection, name, true, index);
    }

    private void ensureIndex(String collection, String name, boolean unique, Index index) {
        IndexOperations indexOperations = mongoTemplate.indexOps(collection);
        for (IndexInfo existing : indexOperations.getIndexInfo()) {
            if (!sameKeysAndDirections(existing, index)) {
                continue;
            }
            if (name.equals(existing.getName()) && unique == existing.isUnique()) {
                return;
            }
            if (unique && existing.isUnique()) {
                // An older deployment may already have an equivalent unique
                // index under a different name; MongoDB rejects a second one.
                log.debug("Unique index on {} already exists as {}", collection, existing.getName());
                return;
            }
            if (unique) {
                throw new IllegalStateException("Existing index " + existing.getName() + " on " + collection
                        + " is not unique; drop it before startup so the unique index can be built");
            }
            log.debug("Non-unique index on {} already exists as {}", collection, existing.getName());
            return;
        }
        long start = System.nanoTime();
        indexOperations.ensureIndex(unique ? index.unique().named(name) : index.named(name));
        log.info("Created {} index {} on {} in {} ms",
                unique ? "unique" : "compound", name, collection,
                java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    private boolean sameKeysAndDirections(IndexInfo existing, Index index) {
        Document keys = index.getIndexKeys();
        List<IndexField> fields = existing.getIndexFields();
        if (fields.size() != keys.size()) {
            return false;
        }
        for (IndexField field : fields) {
            Object direction = keys.get(field.getKey());
            if (!(direction instanceof Number number)) {
                return false;
            }
            Sort.Direction expected = number.intValue() >= 0 ? Sort.Direction.ASC : Sort.Direction.DESC;
            if (field.getDirection() != expected) {
                return false;
            }
        }
        return true;
    }

    private void failOnDuplicates(Map<String, Long> counts, String label) {
        long duplicateKeys = counts.values().stream().filter(count -> count > 1).count();
        if (duplicateKeys > 0) {
            // Counts only: never log the username/email values themselves.
            throw new IllegalStateException("Cannot build the unique " + label
                    + " index: " + duplicateKeys + " normalized value(s) are shared by multiple users");
        }
    }

    private String pairKey(ObjectId first, ObjectId second) {
        if (first == null || second == null) {
            return null;
        }
        return first.toHexString() + ":" + second.toHexString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

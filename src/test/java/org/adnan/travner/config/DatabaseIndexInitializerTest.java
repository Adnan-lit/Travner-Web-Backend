package org.adnan.travner.config;

import org.adnan.travner.entry.CartEntry;
import org.adnan.travner.entry.FollowEntry;
import org.adnan.travner.entry.PostVoteEntry;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexField;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseIndexInitializerTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private IndexOperations indexOperations;

    private DatabaseIndexInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new DatabaseIndexInitializer(mongoTemplate);
    }

    @Test
    void duplicateNormalizedUsernamesFailLoudlyBeforeIndexesAreBuilt() {
        when(mongoTemplate.findAll(Document.class, "users")).thenReturn(List.of(
                user("Alice", "alice@example.com"),
                user("alice", "other@example.com")));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> initializer.run(null));

        assertTrue(ex.getMessage().contains("username"));
        verify(mongoTemplate, never()).indexOps(anyString());
    }

    @Test
    void duplicateNormalizedEmailsFailLoudlyBeforeIndexesAreBuilt() {
        when(mongoTemplate.findAll(Document.class, "users")).thenReturn(List.of(
                user("alice", "ALICE@example.com"),
                user("bob", " alice@example.com ")));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> initializer.run(null));

        assertTrue(ex.getMessage().contains("email"));
        verify(mongoTemplate, never()).indexOps(anyString());
    }

    @Test
    void missingEmailFailsLoudly() {
        when(mongoTemplate.findAll(Document.class, "users")).thenReturn(List.of(user("alice", null)));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> initializer.run(null));

        assertTrue(ex.getMessage().contains("without an email"));
        verify(mongoTemplate, never()).indexOps(anyString());
    }

    @Test
    void usersAreNormalizedAndAllFiveUniqueIndexesAreCreated() {
        when(mongoTemplate.findAll(Document.class, "users")).thenReturn(List.of(
                user(" Alice ", " ALICE@Example.com "),
                user(" BOB ", " BOB@Example.com ")));
        when(mongoTemplate.indexOps(anyString())).thenReturn(indexOperations);

        initializer.run(null);

        ArgumentCaptor<Document> savedUsers = ArgumentCaptor.forClass(Document.class);
        verify(mongoTemplate, times(2)).save(savedUsers.capture(), org.mockito.ArgumentMatchers.eq("users"));
        assertEquals(Set.of("alice", "bob"), savedUsers.getAllValues().stream()
                .map(user -> user.getString("userName")).collect(Collectors.toSet()));
        assertEquals(Set.of("alice@example.com", "bob@example.com"), savedUsers.getAllValues().stream()
                .map(user -> user.getString("email")).collect(Collectors.toSet()));

        ArgumentCaptor<Index> indexes = ArgumentCaptor.forClass(Index.class);
        verify(indexOperations, times(5)).ensureIndex(indexes.capture());
        assertEquals(Set.of("users_userName_unique", "users_email_unique",
                        "follows_follower_following_unique", "post_votes_post_user_unique",
                        "carts_userId_unique"),
                indexes.getAllValues().stream()
                        .map(index -> index.getIndexOptions().getString("name"))
                        .collect(Collectors.toSet()));
        assertTrue(indexes.getAllValues().stream()
                .allMatch(index -> index.getIndexOptions().getBoolean("unique", false)));
        assertTrue(indexes.getAllValues().stream()
                .anyMatch(index -> index.getIndexKeys().keySet().containsAll(List.of("postId", "userId"))));
    }

    @Test
    void duplicateCartsAreMergedAndKeyedByCanonicalUsername() {
        when(mongoTemplate.findAll(Document.class, "users")).thenReturn(List.of());
        when(mongoTemplate.findAll(Document.class, "carts")).thenReturn(List.of(
                rawCart("Alice"), rawCart("alice")));
        when(mongoTemplate.findAll(CartEntry.class)).thenReturn(List.of(cart("Alice"), cart("alice")));
        when(mongoTemplate.indexOps(anyString())).thenReturn(indexOperations);

        initializer.run(null);

        ArgumentCaptor<CartEntry> savedCart = ArgumentCaptor.forClass(CartEntry.class);
        verify(mongoTemplate).save(savedCart.capture());
        assertEquals("alice", savedCart.getValue().getUserId());
        assertEquals(2, savedCart.getValue().getItems().size());
        assertEquals(new BigDecimal("20.00"), savedCart.getValue().getTotalAmount());
        verify(mongoTemplate, times(1)).remove(any(CartEntry.class));
    }

    @Test
    void equivalentLegacyUniqueIndexIsReusedInsteadOfCreatingADuplicate() {
        when(mongoTemplate.findAll(Document.class, "users")).thenReturn(List.of());
        when(mongoTemplate.findAll(Document.class, "carts")).thenReturn(List.of());
        when(mongoTemplate.indexOps(anyString())).thenReturn(indexOperations);
        when(indexOperations.getIndexInfo()).thenReturn(List.of(
                new IndexInfo(List.of(IndexField.create("userName", Sort.Direction.ASC)),
                        "legacy_userName_unique", true, false, null)));

        initializer.run(null);

        ArgumentCaptor<Index> indexes = ArgumentCaptor.forClass(Index.class);
        verify(indexOperations, times(4)).ensureIndex(indexes.capture());
        assertTrue(indexes.getAllValues().stream().noneMatch(index ->
                index.getIndexKeys().containsKey("userName")));
    }

    @Test
    void nonUniqueIndexOnTheSameFieldsAbortsRatherThanSilentlySkippingUniqueness() {
        when(mongoTemplate.findAll(Document.class, "users")).thenReturn(List.of());
        when(mongoTemplate.findAll(Document.class, "carts")).thenReturn(List.of());
        when(mongoTemplate.indexOps(anyString())).thenReturn(indexOperations);
        when(indexOperations.getIndexInfo()).thenReturn(List.of(
                new IndexInfo(List.of(IndexField.create("userName", Sort.Direction.ASC)),
                        "legacy_userName", false, false, null)));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> initializer.run(null));

        assertTrue(ex.getMessage().contains("not unique"));
        verify(indexOperations, never()).ensureIndex(any(Index.class));
    }

    @Test
    void singleMixedCaseCartIsRewrittenEvenWithoutDuplicates() {
        ObjectId cartId = new ObjectId();
        CartEntry stored = CartEntry.builder().id(cartId).userId("alice").build();
        Document raw = new Document("_id", cartId).append("userId", " Alice ");
        when(mongoTemplate.findAll(Document.class, "users")).thenReturn(List.of());
        when(mongoTemplate.findAll(Document.class, "carts")).thenReturn(List.of(raw));
        when(mongoTemplate.findAll(CartEntry.class)).thenReturn(List.of(stored));
        when(mongoTemplate.indexOps(anyString())).thenReturn(indexOperations);

        initializer.run(null);

        ArgumentCaptor<CartEntry> savedCart = ArgumentCaptor.forClass(CartEntry.class);
        verify(mongoTemplate).save(savedCart.capture());
        assertEquals(cartId, savedCart.getValue().getId());
        assertEquals("alice", savedCart.getValue().getUserId());
        verify(mongoTemplate, never()).remove(any(CartEntry.class));
    }

    @Test
    void duplicateFollowsAndVotesAreRemovedBeforeIndexCreation() {
        ObjectId follower = new ObjectId();
        ObjectId following = new ObjectId();
        FollowEntry follow1 = FollowEntry.builder().id(new ObjectId())
                .followerId(follower).followingId(following).createdAt(LocalDateTime.now().minusDays(1)).build();
        FollowEntry follow2 = FollowEntry.builder().id(new ObjectId())
                .followerId(follower).followingId(following).createdAt(LocalDateTime.now()).build();
        ObjectId post = new ObjectId();
        ObjectId voter = new ObjectId();
        PostVoteEntry vote1 = PostVoteEntry.builder().id(new ObjectId())
                .postId(post).userId(voter).createdAt(LocalDateTime.now().minusDays(1)).build();
        PostVoteEntry vote2 = PostVoteEntry.builder().id(new ObjectId())
                .postId(post).userId(voter).createdAt(LocalDateTime.now()).build();

        when(mongoTemplate.findAll(Document.class, "users")).thenReturn(List.of());
        when(mongoTemplate.findAll(Document.class, "carts")).thenReturn(List.of());
        when(mongoTemplate.findAll(CartEntry.class)).thenReturn(List.of());
        when(mongoTemplate.findAll(FollowEntry.class)).thenReturn(List.of(follow1, follow2));
        when(mongoTemplate.findAll(PostVoteEntry.class)).thenReturn(List.of(vote1, vote2));
        when(mongoTemplate.indexOps(anyString())).thenReturn(indexOperations);

        initializer.run(null);

        verify(mongoTemplate).remove(follow2);
        verify(mongoTemplate).remove(vote2);
        verify(indexOperations, times(5)).ensureIndex(any(Index.class));
    }

    private Document rawCart(String userId) {
        Document cart = new Document("_id", new ObjectId());
        cart.put("userId", userId);
        return cart;
    }

    private Document user(String userName, String email) {
        Document user = new Document("userName", userName);
        user.put("email", email);
        return user;
    }

    private CartEntry cart(String userId) {
        CartEntry.CartItem item = CartEntry.CartItem.builder()
                .productId(new ObjectId().toHexString())
                .unitPrice(new BigDecimal("10.00"))
                .quantity(1)
                .subtotal(new BigDecimal("10.00"))
                .build();
        CartEntry cart = CartEntry.builder()
                .id(new ObjectId())
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();
        cart.addItem(item);
        return cart;
    }
}

package org.adnan.travner.domain.conversation;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Conversation entities
 */
public interface ConversationRepository extends MongoRepository<Conversation, ObjectId> {

    /**
     * Find conversations by member ID with pagination, sorted by last message time
     */
    @Query("{ 'memberIds': ?0, 'isArchived': false }")
    Page<Conversation> findByMemberIdsContainingAndIsArchivedFalse(ObjectId memberId, Pageable pageable);

    /**
     * Find direct conversation between exactly two users
     */
    @Query("{ 'type': 'DIRECT', 'memberIds': { $all: [?0, ?1], $size: 2 } }")
    Optional<Conversation> findDirectConversationBetweenUsers(ObjectId userId1, ObjectId userId2);

    /**
     * Find conversations where user is a member
     */
    @Query("{ 'memberIds': ?0 }")
    List<Conversation> findByMemberIdsContaining(ObjectId memberId);

    /**
     * Count unread conversations for a user (based on membership last read time)
     */
    @Query(value = "{ 'memberIds': ?0, 'lastMessageAt': { $gt: ?1 } }", count = true)
    long countUnreadConversations(ObjectId userId, java.time.Instant lastReadAt);
}
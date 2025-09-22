package org.adnan.travner.domain.message;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Message entities
 */
public interface MessageRepository extends MongoRepository<Message, ObjectId> {

        /**
         * Find messages in a conversation with pagination, sorted by creation time
         */
        @Query("{ 'conversationId': ?0, 'deletedAt': null }")
        Page<Message> findByConversationIdAndDeletedAtIsNull(ObjectId conversationId, Pageable pageable);

        /**
         * Find messages created before a specific time (cursor-based pagination)
         */
        @Query("{ 'conversationId': ?0, 'createdAt': { $lt: ?1 }, 'deletedAt': null }")
        List<Message> findByConversationIdAndCreatedAtBeforeAndDeletedAtIsNull(
                        ObjectId conversationId, Instant before, Pageable pageable);

        /**
         * Find messages created after a specific time (cursor-based pagination)
         */
        @Query("{ 'conversationId': ?0, 'createdAt': { $gt: ?1 }, 'deletedAt': null }")
        List<Message> findByConversationIdAndCreatedAtAfterAndDeletedAtIsNull(
                        ObjectId conversationId, Instant after, Pageable pageable);

        /**
         * Find the latest message in a conversation
         */
        @Query(value = "{ 'conversationId': ?0, 'deletedAt': null }", sort = "{ 'createdAt': -1 }")
        Optional<Message> findLatestByConversationId(ObjectId conversationId);

        /**
         * Count unread messages for a user in a conversation
         */
        @Query(value = "{ 'conversationId': ?0, 'createdAt': { $gt: ?1 }, 'deletedAt': null }", count = true)
        long countUnreadMessages(ObjectId conversationId, Instant lastReadAt);

        /**
         * Count total messages in a conversation
         */
        @Query(value = "{ 'conversationId': ?0, 'deletedAt': null }", count = true)
        long countByConversationIdAndDeletedAtIsNull(ObjectId conversationId);

        /**
         * Find messages by sender
         */
        @Query("{ 'senderId': ?0, 'deletedAt': null }")
        List<Message> findBySenderIdAndDeletedAtIsNull(ObjectId senderId);

        /**
         * Find message with reply chain
         */
        @Query("{ 'replyTo': ?0, 'deletedAt': null }")
        List<Message> findRepliesByReplyToAndDeletedAtIsNull(ObjectId messageId);
}
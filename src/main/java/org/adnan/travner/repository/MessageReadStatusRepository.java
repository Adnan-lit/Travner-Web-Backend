package org.adnan.travner.repository;

import org.adnan.travner.domain.message.MessageReadStatus;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for message read status operations
 */
@Repository
public interface MessageReadStatusRepository extends MongoRepository<MessageReadStatus, ObjectId> {

    /**
     * Find read status for a specific message and user
     */
    Optional<MessageReadStatus> findByMessageIdAndUserId(ObjectId messageId, ObjectId userId);

    /**
     * Find all read statuses for a message
     */
    List<MessageReadStatus> findByMessageId(ObjectId messageId);

    /**
     * Find all read statuses for a user in a conversation
     */
    List<MessageReadStatus> findByConversationIdAndUserId(ObjectId conversationId, ObjectId userId);

    /**
     * Count unread messages for a user in a conversation
     */
    @Query(value = "{ 'conversationId': ?0, 'userId': { $ne: ?1 } }", count = true)
    long countUnreadMessagesInConversation(ObjectId conversationId, ObjectId userId);

    /**
     * Get user IDs who have read a specific message
     */
    @Query(value = "{ 'messageId': ?0 }", fields = "{ 'userId': 1 }")
    List<MessageReadStatus> findUserIdsByMessageId(ObjectId messageId);

    /**
     * Delete read statuses for a conversation
     */
    void deleteByConversationId(ObjectId conversationId);

    /**
     * Delete read statuses for a message
     */
    void deleteByMessageId(ObjectId messageId);
}
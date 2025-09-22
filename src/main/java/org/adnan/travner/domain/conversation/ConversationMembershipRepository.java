package org.adnan.travner.domain.conversation;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ConversationMembership entities
 */
public interface ConversationMembershipRepository extends MongoRepository<ConversationMembership, ObjectId> {

    /**
     * Find membership by conversation and user
     */
    Optional<ConversationMembership> findByConversationIdAndUserId(ObjectId conversationId, ObjectId userId);

    /**
     * Find all memberships for a conversation
     */
    List<ConversationMembership> findByConversationId(ObjectId conversationId);

    /**
     * Find all memberships for a user
     */
    List<ConversationMembership> findByUserId(ObjectId userId);

    /**
     * Check if user is member of conversation
     */
    boolean existsByConversationIdAndUserId(ObjectId conversationId, ObjectId userId);

    /**
     * Find members with specific roles in a conversation
     */
    @Query("{ 'conversationId': ?0, 'role': { $in: ?1 } }")
    List<ConversationMembership> findByConversationIdAndRoleIn(ObjectId conversationId,
            List<ConversationMembership.MemberRole> roles);

    /**
     * Delete all memberships for a conversation
     */
    void deleteByConversationId(ObjectId conversationId);

    /**
     * Count members in a conversation
     */
    long countByConversationId(ObjectId conversationId);

    /**
     * Delete membership by conversation and user
     */
    void deleteByConversationIdAndUserId(ObjectId conversationId, ObjectId userId);
}
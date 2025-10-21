package org.adnan.travner.repository;

import org.adnan.travner.entry.ChatConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ChatConversation entity
 */
@Repository
public interface ChatConversationRepository extends MongoRepository<ChatConversation, String> {
    
    /**
     * Find conversations by participant ID and active status
     */
    Page<ChatConversation> findByParticipantIdsContainingAndIsActiveTrue(String participantId, Pageable pageable);
    
    /**
     * Find conversations by participant ID and type
     */
    List<ChatConversation> findByParticipantIdsContainingAndType(String participantId, String type);
    
    /**
     * Find direct conversation between two users
     */
    @Query("{ 'type': 'DIRECT', 'participantIds': { $all: [?0, ?1] }, 'participantIds': { $size: 2 } }")
    Optional<ChatConversation> findDirectConversationBetweenUsers(String userId1, String userId2);
    
    /**
     * Find conversations by participant ID
     */
    List<ChatConversation> findByParticipantIdsContaining(String participantId);
    
    /**
     * Find active conversations by participant ID
     */
    List<ChatConversation> findByParticipantIdsContainingAndIsActiveTrue(String participantId);
    
    /**
     * Find conversations by title (for group conversations)
     */
    List<ChatConversation> findByTitleContainingIgnoreCaseAndIsActiveTrue(String title);
}
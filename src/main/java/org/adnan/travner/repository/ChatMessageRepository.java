package org.adnan.travner.repository;

import org.adnan.travner.entry.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

/**
 * Repository for ChatMessage entity
 */
@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    
    /**
     * Find messages by conversation ID, ordered by creation date descending
     */
    Page<ChatMessage> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);
    
    /**
     * Find messages by conversation ID
     */
    List<ChatMessage> findByConversationId(String conversationId);
    
    /**
     * Find unread messages for a user in a conversation
     */
    @Query("{ 'conversationId': ?0, 'readBy': { $nin: [?1] } }")
    List<ChatMessage> findByConversationIdAndReadByNotContaining(String conversationId, String userId);
    
    /**
     * Count unread messages for a user in a conversation
     */
    @Query(value = "{ 'conversationId': ?0, 'readBy': { $nin: [?1] } }", count = true)
    long countByConversationIdAndReadByNotContaining(String conversationId, String userId);
    
    /**
     * Find messages by conversation ID and sender ID
     */
    List<ChatMessage> findByConversationIdAndSenderId(String conversationId, String senderId);
    
    // Analytics methods
    long countBySenderId(String senderId);
    long countByCreatedAtAfter(LocalDateTime date);
    List<String> findDistinctConversationIdsByCreatedAtAfter(LocalDateTime date);
    List<ChatMessage> findByCreatedAtAfter(LocalDateTime date);
}
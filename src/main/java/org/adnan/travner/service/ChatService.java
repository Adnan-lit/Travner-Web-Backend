package org.adnan.travner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.*;
import org.adnan.travner.entry.ChatMessage;
import org.adnan.travner.entry.ChatConversation;
import org.adnan.travner.repository.ChatMessageRepository;
import org.adnan.travner.repository.ChatConversationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final UserService userService;

    /**
     * Get or create a direct conversation between two users
     */
    public ChatConversationDTO getOrCreateDirectConversation(String currentUserId, String otherUserId) {
        log.debug("Getting or creating direct conversation between: {} and {}", currentUserId, otherUserId);
        
        // Check if conversation already exists
        Optional<ChatConversation> existingConversation = chatConversationRepository
                .findDirectConversationBetweenUsers(currentUserId, otherUserId);

        if (existingConversation.isPresent()) {
            return convertToConversationDTO(existingConversation.get());
        }

        // Create new conversation
        ChatConversation newConversation = ChatConversation.builder()
                .participantIds(List.of(currentUserId, otherUserId))
                .type("DIRECT")
                .title(generateDirectConversationTitle(currentUserId, otherUserId))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .unreadCount(0)
                .isActive(true)
                .build();

        ChatConversation savedConversation = chatConversationRepository.save(newConversation);
        log.debug("Created new direct conversation: {}", savedConversation.getId());
        
        return convertToConversationDTO(savedConversation);
    }

    /**
     * Get user's conversations
     */
    public Page<ChatConversationDTO> getUserConversations(String userId, Pageable pageable) {
        log.debug("Getting conversations for user: {}", userId);
        
        Page<ChatConversation> conversations = chatConversationRepository
                .findByParticipantIdsContainingAndIsActiveTrue(userId, pageable);
        
        return conversations.map(this::convertToConversationDTO);
    }

    /**
     * Get messages for a conversation
     */
    public Page<ChatMessageDTO> getConversationMessages(String conversationId, Pageable pageable) {
        log.debug("Getting messages for conversation: {}", conversationId);
        
        Page<ChatMessage> messages = chatMessageRepository
                .findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        
        return messages.map(this::convertToMessageDTO);
    }

    /**
     * Send a message
     */
    public ChatMessageDTO sendMessage(SendMessageDTO request, String senderId) {
        log.debug("Sending message from user: {} to conversation: {}", senderId, request.getConversationId());
        
        // Verify conversation exists and user is participant
        ChatConversation conversation = chatConversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        if (!conversation.getParticipantIds().contains(senderId)) {
            throw new RuntimeException("User is not a participant in this conversation");
        }

        // Get sender user details
        var senderUser = userService.getById(senderId);
        String senderUsername = senderUser != null ? senderUser.getUserName() : "Unknown User";
        
        // Create and save message
        ChatMessage message = ChatMessage.builder()
                .conversationId(request.getConversationId())
                .senderId(senderId)
                .senderUsername(senderUsername)
                .content(request.getContent())
                .messageType(request.getMessageType())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isEdited(false)
                .replyToMessageId(request.getReplyToMessageId())
                .readBy(List.of(senderId)) // Sender has read their own message
                .status("DELIVERED")
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);
        
        // Update conversation last message and unread counts
        conversation.setLastMessage(request.getContent());
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        
        // Update unread counts for all participants except sender
        for (String participantId : conversation.getParticipantIds()) {
            if (!participantId.equals(senderId)) {
                long unreadCount = chatMessageRepository.countByConversationIdAndReadByNotContaining(conversation.getId(), participantId);
                // Update conversation unread count (this is a simplified approach)
                // In a real implementation, you might want to track per-user unread counts
            }
        }
        
        chatConversationRepository.save(conversation);
        
        log.debug("Message sent successfully: {}", savedMessage.getId());
        return convertToMessageDTO(savedMessage);
    }

    /**
     * Mark messages as read
     */
    public void markMessagesAsRead(String conversationId, String userId) {
        log.debug("Marking messages as read for user: {} in conversation: {}", userId, conversationId);
        
        List<ChatMessage> unreadMessages = chatMessageRepository
                .findByConversationIdAndReadByNotContaining(conversationId, userId);
        
        for (ChatMessage message : unreadMessages) {
            if (!message.getReadBy().contains(userId)) {
                message.getReadBy().add(userId);
                if (message.getReadBy().size() == 2) { // All participants read
                    message.setStatus("READ");
                }
                chatMessageRepository.save(message);
            }
        }
    }

    /**
     * Get conversation by ID
     */
    public ChatConversationDTO getConversationById(String conversationId) {
        ChatConversation conversation = chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        return convertToConversationDTO(conversation);
    }

    // Helper methods
    private String generateDirectConversationTitle(String currentUserId, String otherUserId) {
        try {
            var otherUser = userService.getById(otherUserId);
            
            if (otherUser != null) {
                // Return the username of the other person
                return otherUser.getUserName();
            }
        } catch (Exception e) {
            log.warn("Error generating conversation title: {}", e.getMessage());
        }
        
        return "Direct Message";
    }

    private ChatConversationDTO convertToConversationDTO(ChatConversation conversation) {
        return ChatConversationDTO.builder()
                .id(conversation.getId())
                .participantIds(conversation.getParticipantIds())
                .title(conversation.getTitle())
                .type(conversation.getType())
                .lastMessage(conversation.getLastMessage())
                .lastMessageAt(conversation.getLastMessageAt())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .unreadCount(conversation.getUnreadCount())
                .isActive(conversation.isActive())
                .build();
    }

    private ChatMessageDTO convertToMessageDTO(ChatMessage message) {
        return ChatMessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .senderUsername(message.getSenderUsername())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .isEdited(message.isEdited())
                .replyToMessageId(message.getReplyToMessageId())
                .readBy(message.getReadBy())
                .status(message.getStatus())
                            .build();
    }
}
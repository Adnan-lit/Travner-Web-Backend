package org.adnan.travner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.*;
import org.adnan.travner.dto.chat.ChatParticipantDTO;
import org.adnan.travner.entry.ChatMessage;
import org.adnan.travner.entry.ChatConversation;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.ChatMessageRepository;
import org.adnan.travner.repository.ChatConversationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    /** Neutral label for deleted/unresolvable accounts — never a raw ObjectId. */
    private static final String FORMER_USER = "Former user";

    private final ChatMessageRepository chatMessageRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final UserService userService;

    /**
     * Get or create a direct conversation between two users. Both arguments
     * are user IDs (JWT subjects), never display usernames.
     */
    public ChatConversationDTO getOrCreateDirectConversation(String currentUserId, String otherUserId) {
        log.debug("Getting or creating direct conversation between: {} and {}", currentUserId, otherUserId);

        Optional<ChatConversation> existingConversation = chatConversationRepository
                .findDirectConversationBetweenUsers(currentUserId, otherUserId);

        if (existingConversation.isPresent()) {
            return convertToConversationDTO(existingConversation.get(), currentUserId);
        }

        ChatConversation newConversation = ChatConversation.builder()
                .participantIds(List.of(currentUserId, otherUserId))
                .type("DIRECT")
                .title(generateDirectConversationTitle(otherUserId))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .unreadCount(0)
                .isActive(true)
                .build();

        ChatConversation savedConversation = chatConversationRepository.save(newConversation);
        log.debug("Created new direct conversation: {}", savedConversation.getId());

        return convertToConversationDTO(savedConversation, currentUserId);
    }

    /**
     * Get user's conversations. Unread counts are computed per participant;
     * the legacy {@code unreadCount} field reflects the requesting user.
     */
    public Page<ChatConversationDTO> getUserConversations(String userId, Pageable pageable) {
        log.debug("Getting conversations for user: {}", userId);

        Page<ChatConversation> conversations = chatConversationRepository
                .findByParticipantIdsContainingAndIsActiveTrue(userId, pageable);

        return conversations.map(conversation -> convertToConversationDTO(conversation, userId));
    }

    /**
     * Get messages for a conversation. Participant membership is enforced on
     * the stable user ID before any message data is returned.
     */
    public Page<ChatMessageDTO> getConversationMessages(String conversationId, String userId, Pageable pageable) {
        log.debug("Getting messages for conversation: {}", conversationId);
        assertParticipant(conversationId, userId);

        Page<ChatMessage> messages = chatMessageRepository
                .findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);

        return messages.map(this::convertToMessageDTO);
    }

    /** Send a message. {@code senderId} is the authenticated user's ID. */
    public ChatMessageDTO sendMessage(SendMessageDTO request, String senderId) {
        log.debug("Sending message from user: {} to conversation: {}", senderId, request.getConversationId());

        ChatConversation conversation = requireConversation(request.getConversationId());
        assertParticipant(conversation, senderId);

        ChatParticipantDTO sender = resolveParticipant(senderId, null);

        ChatMessage message = ChatMessage.builder()
                .conversationId(request.getConversationId())
                .senderId(senderId)
                .senderUsername(sender.getDisplayName())
                .content(request.getContent())
                .messageType(request.getMessageType())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isEdited(false)
                .replyToMessageId(request.getReplyToMessageId())
                .readBy(new java.util.ArrayList<>(List.of(senderId))) // sender has read their own message
                .status("DELIVERED")
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);

        conversation.setLastMessage(request.getContent());
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        chatConversationRepository.save(conversation);

        log.debug("Message sent successfully: {}", savedMessage.getId());
        return convertToMessageDTO(savedMessage);
    }

    /** Mark messages as read for one participant. */
    public void markMessagesAsRead(String conversationId, String userId) {
        log.debug("Marking messages as read for user: {} in conversation: {}", userId, conversationId);
        ChatConversation conversation = requireConversation(conversationId);
        assertParticipant(conversation, userId);

        List<ChatMessage> unreadMessages = chatMessageRepository
                .findByConversationIdAndReadByNotContaining(conversationId, userId);

        int participantCount = conversation.getParticipantIds() == null ? 0 : conversation.getParticipantIds().size();
        for (ChatMessage message : unreadMessages) {
            if (!message.getReadBy().contains(userId)) {
                message.getReadBy().add(userId);
                if (participantCount > 0 && message.getReadBy().size() >= participantCount) {
                    message.setStatus("READ");
                }
                chatMessageRepository.save(message);
            }
        }
    }

    /** Get conversation by ID, enforcing membership on the requesting user. */
    public ChatConversationDTO getConversationById(String conversationId, String userId) {
        ChatConversation conversation = requireConversation(conversationId);
        assertParticipant(conversation, userId);
        return convertToConversationDTO(conversation, userId);
    }

    /** Callable from REST and STOMP handlers before touching a conversation. */
    public void assertParticipant(String conversationId, String userId) {
        assertParticipant(requireConversation(conversationId), userId);
    }

    private ChatConversation requireConversation(String conversationId) {
        return chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new org.adnan.travner.exception.ResourceNotFoundException(
                        "Conversation not found"));
    }

    private void assertParticipant(ChatConversation conversation, String userId) {
        if (conversation.getParticipantIds() == null
                || !conversation.getParticipantIds().contains(userId)) {
            throw new org.adnan.travner.exception.ForbiddenException(
                    "User is not a participant in this conversation");
        }
    }

    private String generateDirectConversationTitle(String otherUserId) {
        ChatParticipantDTO otherUser = resolveParticipant(otherUserId, null);
        return otherUser.getDisplayName();
    }

    private ChatConversationDTO convertToConversationDTO(ChatConversation conversation, String viewerId) {
        List<String> participantIds = conversation.getParticipantIds() == null
                ? List.of() : conversation.getParticipantIds();

        Map<String, Integer> unreadCounts = new HashMap<>();
        for (String participantId : participantIds) {
            unreadCounts.put(participantId, (int) chatMessageRepository
                    .countByConversationIdAndReadByNotContaining(conversation.getId(), participantId));
        }

        return ChatConversationDTO.builder()
                .id(conversation.getId())
                .participantIds(participantIds)
                .participants(participantIds.stream().map(this::resolveParticipant).toList())
                .title(conversation.getTitle())
                .type(conversation.getType())
                .lastMessage(conversation.getLastMessage())
                .lastMessageAt(conversation.getLastMessageAt())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .unreadCounts(unreadCounts)
                .unreadCount(viewerId == null ? 0 : unreadCounts.getOrDefault(viewerId, 0))
                .isActive(conversation.isActive())
                .build();
    }

    private ChatMessageDTO convertToMessageDTO(ChatMessage message) {
        ChatParticipantDTO sender = resolveParticipant(message.getSenderId(), message.getSenderUsername());
        return ChatMessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .senderUsername(sender.getDisplayName())
                .sender(sender)
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

    private ChatParticipantDTO resolveParticipant(String userId) {
        return resolveParticipant(userId, null);
    }

    /**
     * Resolve a user ID to display-safe data. Falls back to the name captured
     * on the message (if any) for deleted accounts; never exposes a raw ID or
     * the "Unknown User" literal.
     */
    private ChatParticipantDTO resolveParticipant(String userId, String storedUsername) {
        String username = null;
        String displayName = null;
        String avatarUrl = null;
        if (userId != null) {
            UserEntry user = userService.getById(userId);
            if (user != null) {
                username = user.getUserName();
                displayName = displayName(user);
                avatarUrl = user.getProfileImageUrl();
            }
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = (storedUsername != null && !storedUsername.isBlank())
                    ? storedUsername : FORMER_USER;
        }
        if (username == null || username.isBlank()) {
            username = displayName;
        }
        return ChatParticipantDTO.builder()
                .id(userId)
                .username(username)
                .displayName(displayName)
                .avatarUrl(avatarUrl)
                .build();
    }

    private String displayName(UserEntry user) {
        String first = user.getFirstName();
        String last = user.getLastName();
        if (first != null && !first.isBlank()) {
            return (last != null && !last.isBlank()) ? first + " " + last : first;
        }
        return user.getUserName();
    }
}

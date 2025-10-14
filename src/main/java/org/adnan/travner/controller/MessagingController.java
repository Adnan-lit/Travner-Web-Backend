package org.adnan.travner.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.dto.chat.*;
import org.adnan.travner.service.ConversationService;
import org.adnan.travner.service.MessageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for messaging (conversations and messages)
 * Provides simplified endpoints for Postman testing
 */
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class MessagingController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    /**
     * Get user's conversations
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getUserConversations(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            log.debug("Getting conversations for user: {}", authentication.getName());

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastMessageAt"));
            Page<ConversationResponse> conversationsPage = conversationService.getUserConversations(
                    authentication.getName(), pageable);

            return ResponseEntity.ok(ApiResponse.success(
                    "Conversations retrieved successfully",
                    conversationsPage.getContent()));
        } catch (Exception e) {
            log.error("Error getting conversations for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get conversations: " + e.getMessage()));
        }
    }

    /**
     * Start a new conversation
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse>> startConversation(
            Authentication authentication,
            @Valid @RequestBody CreateConversationRequest request) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            log.debug("Creating conversation for user: {}", authentication.getName());

            ConversationResponse conversation = conversationService.createConversation(
                    request, authentication.getName());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Conversation created successfully", conversation));
        } catch (Exception e) {
            log.error("Error creating conversation for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create conversation: " + e.getMessage()));
        }
    }

    /**
     * Get messages in a conversation
     */
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getConversationMessages(
            Authentication authentication,
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            log.debug("Getting messages for conversation: {}", conversationId);

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<MessageResponse> messagesPage = messageService.getMessages(
                    conversationId, authentication.getName(), pageable);

            return ResponseEntity.ok(ApiResponse.success(
                    "Messages retrieved successfully",
                    messagesPage.getContent()));
        } catch (Exception e) {
            log.error("Error getting messages for conversation {}: {}", conversationId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get messages: " + e.getMessage()));
        }
    }

    /**
     * Send a message in a conversation
     */
    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            Authentication authentication,
            @PathVariable String conversationId,
            @Valid @RequestBody SendMessageRequest request) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            log.debug("Sending message to conversation: {}", conversationId);

            // Set conversationId from path variable
            request.setConversationId(conversationId);

            MessageResponse message = messageService.sendMessage(request, authentication.getName());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Message sent successfully", message));
        } catch (Exception e) {
            log.error("Error sending message to conversation {}: {}", conversationId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to send message: " + e.getMessage()));
        }
    }

    /**
     * Mark messages as read
     */
    @PutMapping("/{conversationId}/read")
    public ResponseEntity<ApiResponse<Object>> markAsRead(
            Authentication authentication,
            @PathVariable String conversationId,
            @RequestParam(required = false) String lastReadMessageId) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            log.debug("Marking messages as read in conversation: {}", conversationId);

            if (lastReadMessageId != null) {
                messageService.markMessagesAsRead(conversationId, lastReadMessageId, authentication.getName());
            }

            return ResponseEntity.ok(ApiResponse.success("Messages marked as read", null));
        } catch (Exception e) {
            log.error("Error marking messages as read in conversation {}: {}", conversationId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to mark messages as read: " + e.getMessage()));
        }
    }
}

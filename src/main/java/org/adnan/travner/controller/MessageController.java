package org.adnan.travner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.dto.chat.MarkMessagesReadRequest;
import org.adnan.travner.dto.chat.MessageResponse;
import org.adnan.travner.dto.chat.SendMessageRequest;
import org.adnan.travner.service.MessageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for message management
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Messages", description = "Message management APIs")
public class MessageController {

        private final MessageService messageService;

        /**
         * Send a message in a conversation
         */
        @PostMapping("/messages")
        @Operation(summary = "Send message", description = "Send a message in a conversation")
        public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
                        @Valid @RequestBody SendMessageRequest request,
                        Authentication authentication) {

                log.debug("Sending message in conversation: {} from user: {}",
                                request.getConversationId(), authentication.getName());

                MessageResponse message = messageService.sendMessage(request, authentication.getName());

                ApiResponse<MessageResponse> response = ApiResponse.<MessageResponse>builder()
                                .success(true)
                                .message("Message sent successfully")
                                .data(message)
                                .build();

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        /**
         * Get messages for a conversation
         */
        @GetMapping("/conversations/{conversationId}/messages")
        @Operation(summary = "Get messages", description = "Get messages for a conversation with pagination")
        public ResponseEntity<ApiResponse<Page<MessageResponse>>> getMessages(
                        @Parameter(description = "Conversation ID") @PathVariable String conversationId,
                        @Parameter(description = "Pagination parameters") @PageableDefault(size = 50, sort = "createdAt") Pageable pageable,
                        Authentication authentication) {

                log.debug("Getting messages for conversation: {} user: {}", conversationId, authentication.getName());

                Page<MessageResponse> messages = messageService.getMessages(conversationId, authentication.getName(),
                                pageable);

                ApiResponse<Page<MessageResponse>> response = ApiResponse.<Page<MessageResponse>>builder()
                                .success(true)
                                .message("Messages retrieved successfully")
                                .data(messages)
                                .pagination(ApiResponse.PaginationMeta.builder()
                                                .page(messages.getNumber())
                                                .size(messages.getSize())
                                                .totalElements(messages.getTotalElements())
                                                .totalPages(messages.getTotalPages())
                                                .first(messages.isFirst())
                                                .last(messages.isLast())
                                                .build())
                                .build();

                return ResponseEntity.ok(response);
        }

        /**
         * Edit a message
         */
        @PutMapping("/messages/{messageId}")
        @Operation(summary = "Edit message", description = "Edit an existing message")
        public ResponseEntity<ApiResponse<MessageResponse>> editMessage(
                        @Parameter(description = "Message ID") @PathVariable String messageId,
                        @Parameter(description = "New message content") @RequestParam String content,
                        Authentication authentication) {

                log.debug("Editing message: {} by user: {}", messageId, authentication.getName());

                MessageResponse message = messageService.editMessage(messageId, content, authentication.getName());

                ApiResponse<MessageResponse> response = ApiResponse.<MessageResponse>builder()
                                .success(true)
                                .message("Message edited successfully")
                                .data(message)
                                .build();

                return ResponseEntity.ok(response);
        }

        /**
         * Delete a message
         */
        @DeleteMapping("/messages/{messageId}")
        @Operation(summary = "Delete message", description = "Delete a message")
        public ResponseEntity<ApiResponse<Void>> deleteMessage(
                        @Parameter(description = "Message ID") @PathVariable String messageId,
                        Authentication authentication) {

                log.debug("Deleting message: {} by user: {}", messageId, authentication.getName());

                messageService.deleteMessage(messageId, authentication.getName());

                ApiResponse<Void> response = ApiResponse.<Void>builder()
                                .success(true)
                                .message("Message deleted successfully")
                                .build();

                return ResponseEntity.ok(response);
        }

        /**
         * Mark messages as read
         */
        @PostMapping("/messages/read")
        @Operation(summary = "Mark messages read", description = "Mark messages as read up to a specific message")
        public ResponseEntity<ApiResponse<Void>> markMessagesAsRead(
                        @Valid @RequestBody MarkMessagesReadRequest request,
                        Authentication authentication) {

                log.debug("Marking messages as read in conversation: {} up to message: {} for user: {}",
                                request.getConversationId(), request.getLastReadMessageId(), authentication.getName());

                messageService.markMessagesAsRead(
                                request.getConversationId(),
                                request.getLastReadMessageId(),
                                authentication.getName());

                ApiResponse<Void> response = ApiResponse.<Void>builder()
                                .success(true)
                                .message("Messages marked as read")
                                .build();

                return ResponseEntity.ok(response);
        }

        /**
         * Get unread message count for a conversation
         */
        @GetMapping("/conversations/{conversationId}/unread-count")
        @Operation(summary = "Get unread count", description = "Get unread message count for a conversation")
        public ResponseEntity<ApiResponse<Long>> getUnreadCount(
                        @Parameter(description = "Conversation ID") @PathVariable String conversationId,
                        Authentication authentication) {

                log.debug("Getting unread count for conversation: {} user: {}", conversationId,
                                authentication.getName());

                long unreadCount = messageService.getUnreadCount(conversationId, authentication.getName());

                ApiResponse<Long> response = ApiResponse.<Long>builder()
                                .success(true)
                                .message("Unread count retrieved successfully")
                                .data(unreadCount)
                                .build();

                return ResponseEntity.ok(response);
        }
}
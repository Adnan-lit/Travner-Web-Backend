package org.adnan.travner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.dto.chat.*;
import org.adnan.travner.service.ConversationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for conversation management
 */
@RestController
// Allow both legacy and current API paths for backward compatibility
@RequestMapping({"/api/chat/conversations", "/api/conversations"})
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Conversations", description = "Conversation management APIs")
public class ConversationController {

        private final ConversationService conversationService;

        /**
         * Create a new conversation
         */
        @PostMapping
        @Operation(summary = "Create conversation", description = "Create a new direct or group conversation")
        public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
                        @Valid @RequestBody CreateConversationRequest request,
                        Authentication authentication) {

                log.debug("Creating conversation for user: {}", authentication.getName());

                ConversationResponse conversation = conversationService.createConversation(request,
                                authentication.getName());

                ApiResponse<ConversationResponse> response = ApiResponse.<ConversationResponse>builder()
                                .success(true)
                                .message("Conversation created successfully")
                                .data(conversation)
                                .build();

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        /**
         * Get or create a DIRECT (one-to-one) conversation between the authenticated
         * user and the specified user
         */
        @GetMapping("/direct/{otherUserId}")
        @Operation(summary = "Get or create DIRECT conversation", description = "Fetch an existing one-to-one conversation with the specified user or create it if it doesn't exist")
        public ResponseEntity<ApiResponse<ConversationResponse>> getOrCreateDirectConversation(
                        @Parameter(description = "Other user's ID or username") @PathVariable String otherUserId,
                        Authentication authentication) {

                log.debug("Get or create DIRECT conversation between {} and {}", authentication.getName(),
                                otherUserId);

                // Delegate to the same create flow; service will reuse if exists
                var req = CreateConversationRequest.builder()
                                .type(org.adnan.travner.domain.conversation.Conversation.ConversationType.DIRECT)
                                .memberIds(java.util.List.of(otherUserId))
                                .build();

                ConversationResponse conversation = conversationService.createConversation(req,
                                authentication.getName());

                ApiResponse<ConversationResponse> response = ApiResponse.<ConversationResponse>builder()
                                .success(true)
                                .message("DIRECT conversation ready")
                                .data(conversation)
                                .build();

                return ResponseEntity.ok(response);
        }

        /**
         * Get user's conversations with pagination
         */
        @GetMapping
        @Operation(summary = "Get conversations", description = "Get user's conversations with pagination")
        public ResponseEntity<ApiResponse<Page<ConversationResponse>>> getConversations(
                        @Parameter(description = "Pagination parameters") @PageableDefault(size = 20, sort = "lastMessageAt") Pageable pageable,
                        Authentication authentication) {

                log.debug("Getting conversations for user: {}", authentication.getName());

                Page<ConversationResponse> conversations = conversationService.getUserConversations(
                                authentication.getName(), pageable);

                ApiResponse<Page<ConversationResponse>> response = ApiResponse.<Page<ConversationResponse>>builder()
                                .success(true)
                                .message("Conversations retrieved successfully")
                                .data(conversations)
                                .pagination(ApiResponse.PaginationMeta.builder()
                                                .page(conversations.getNumber())
                                                .size(conversations.getSize())
                                                .totalElements(conversations.getTotalElements())
                                                .totalPages(conversations.getTotalPages())
                                                .first(conversations.isFirst())
                                                .last(conversations.isLast())
                                                .build())
                                .build();

                return ResponseEntity.ok(response);
        }

        /**
         * Get conversation by ID
         */
        @GetMapping("/{conversationId}")
        @Operation(summary = "Get conversation", description = "Get conversation details by ID")
        public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(
                        @Parameter(description = "Conversation ID") @PathVariable String conversationId,
                        Authentication authentication) {

                log.debug("Getting conversation: {} for user: {}", conversationId, authentication.getName());

                ConversationResponse conversation = conversationService.getConversation(conversationId,
                                authentication.getName());

                ApiResponse<ConversationResponse> response = ApiResponse.<ConversationResponse>builder()
                                .success(true)
                                .message("Conversation retrieved successfully")
                                .data(conversation)
                                .build();

                return ResponseEntity.ok(response);
        }

        /**
         * Add members to conversation
         */
        @PostMapping("/{conversationId}/members")
        @Operation(summary = "Add members", description = "Add members to a group conversation")
        public ResponseEntity<ApiResponse<Void>> addMembers(
                        @Parameter(description = "Conversation ID") @PathVariable String conversationId,
                        @Valid @RequestBody AddMembersRequest request,
                        Authentication authentication) {

                log.debug("Adding members to conversation: {} by user: {}", conversationId, authentication.getName());

                // Set conversation ID from path parameter
                request.setConversationId(conversationId);

                conversationService.addMembers(conversationId, request.getUserIds(), authentication.getName());

                ApiResponse<Void> response = ApiResponse.<Void>builder()
                                .success(true)
                                .message("Members added successfully")
                                .build();

                return ResponseEntity.ok(response);
        }

        /**
         * Remove member from conversation
         */
        @DeleteMapping("/{conversationId}/members/{userId}")
        @Operation(summary = "Remove member", description = "Remove a member from conversation")
        public ResponseEntity<ApiResponse<Void>> removeMember(
                        @Parameter(description = "Conversation ID") @PathVariable String conversationId,
                        @Parameter(description = "User ID to remove") @PathVariable String userId,
                        Authentication authentication) {

                log.debug("Removing member: {} from conversation: {} by user: {}",
                                userId, conversationId, authentication.getName());

                conversationService.removeMember(conversationId, userId, authentication.getName());

                ApiResponse<Void> response = ApiResponse.<Void>builder()
                                .success(true)
                                .message("Member removed successfully")
                                .build();

                return ResponseEntity.ok(response);
        }
}
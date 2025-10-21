package org.adnan.travner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.*;
import org.adnan.travner.service.ChatService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat", description = "Chat management APIs")
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Get user's conversations
     */
    @GetMapping("/conversations")
    @Operation(summary = "Get user conversations", description = "Get paginated list of user's conversations")
    public ResponseEntity<ApiResponse<Page<ChatConversationDTO>>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        String userId = authentication.getName();
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastMessageAt").descending());
        
        Page<ChatConversationDTO> conversations = chatService.getUserConversations(userId, pageable);
        
        ApiResponse<Page<ChatConversationDTO>> response = ApiResponse.<Page<ChatConversationDTO>>builder()
                .success(true)
                .message("Conversations retrieved successfully")
                .data(conversations)
                .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get or create direct conversation
     */
    @GetMapping("/conversations/direct/{otherUserId}")
    @Operation(summary = "Get or create direct conversation", description = "Get or create direct conversation with another user")
    public ResponseEntity<ApiResponse<ChatConversationDTO>> getOrCreateDirectConversation(
            @PathVariable String otherUserId,
            Authentication authentication) {
        
        String currentUserId = authentication.getName();
        ChatConversationDTO conversation = chatService.getOrCreateDirectConversation(currentUserId, otherUserId);
        
        ApiResponse<ChatConversationDTO> response = ApiResponse.<ChatConversationDTO>builder()
                .success(true)
                .message("Direct conversation retrieved/created successfully")
                .data(conversation)
                .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get conversation messages
     */
    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Get conversation messages", description = "Get paginated messages for a conversation")
    public ResponseEntity<ApiResponse<Page<ChatMessageDTO>>> getConversationMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        
        String userId = authentication.getName();
        
        // Verify user is participant
        ChatConversationDTO conversation = chatService.getConversationById(conversationId);
        if (!conversation.getParticipantIds().contains(userId)) {
            return ResponseEntity.badRequest().body(
                ApiResponse.<Page<ChatMessageDTO>>builder()
                    .success(false)
                    .message("User is not a participant in this conversation")
                    .build()
            );
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ChatMessageDTO> messages = chatService.getConversationMessages(conversationId, pageable);
        
        ApiResponse<Page<ChatMessageDTO>> response = ApiResponse.<Page<ChatMessageDTO>>builder()
                .success(true)
                .message("Messages retrieved successfully")
                .data(messages)
                .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Send message via REST API
     */
    @PostMapping("/messages")
    @Operation(summary = "Send message", description = "Send a message to a conversation")
    public ResponseEntity<ApiResponse<ChatMessageDTO>> sendMessage(
            @RequestBody SendMessageDTO request,
            Authentication authentication) {
        
        String senderId = authentication.getName();
        ChatMessageDTO message = chatService.sendMessage(request, senderId);
        
        // Broadcast message to conversation participants via WebSocket
        messagingTemplate.convertAndSend("/topic/conversation/" + request.getConversationId(), message);
        
        ApiResponse<ChatMessageDTO> response = ApiResponse.<ChatMessageDTO>builder()
                .success(true)
                .message("Message sent successfully")
                .data(message)
                .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Mark messages as read
     */
    @PostMapping("/conversations/{conversationId}/mark-read")
    @Operation(summary = "Mark messages as read", description = "Mark all messages in a conversation as read")
    public ResponseEntity<ApiResponse<Void>> markMessagesAsRead(
            @PathVariable String conversationId,
            Authentication authentication) {
        
        String userId = authentication.getName();
        chatService.markMessagesAsRead(conversationId, userId);
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Messages marked as read")
                .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * WebSocket endpoint for sending messages
     */
    @MessageMapping("/chat.sendMessage")
    public void handleSendMessage(@Payload SendMessageDTO request, Principal principal) {
        String senderId = principal.getName();
        log.debug("Received WebSocket message from user: {} to conversation: {}", senderId, request.getConversationId());
        
        try {
            ChatMessageDTO message = chatService.sendMessage(request, senderId);
            
            // Broadcast to conversation participants
            messagingTemplate.convertAndSend("/topic/conversation/" + request.getConversationId(), message);
            
            // Send to user-specific queue for notifications
            messagingTemplate.convertAndSendToUser(senderId, "/queue/messages", message);
            
        } catch (Exception e) {
            log.error("Error handling WebSocket message: {}", e.getMessage(), e);
        }
    }

    /**
     * WebSocket endpoint for typing indicators
     */
    @MessageMapping("/chat.typing")
    public void handleTypingIndicator(@Payload TypingIndicatorDTO typingIndicator, Principal principal) {
        String senderId = principal.getName();
        log.debug("Received typing indicator from user: {} in conversation: {}", senderId, typingIndicator.getConversationId());
        
        // Set the user info
        typingIndicator.setUserId(senderId);
        typingIndicator.setUsername(principal.getName());
        
        // Broadcast typing indicator to conversation participants
        messagingTemplate.convertAndSend("/topic/conversation/" + typingIndicator.getConversationId() + "/typing", typingIndicator);
    }

    /**
     * Upload file for chat
     */
    @PostMapping("/upload")
    @Operation(summary = "Upload file for chat", description = "Upload a file to be shared in a chat conversation")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("conversationId") String conversationId,
            Authentication authentication) {
        
        String userId = authentication.getName();
        log.debug("Uploading file for user: {} in conversation: {}", userId, conversationId);
        
        try {
            // Verify user is participant in conversation
            ChatConversationDTO conversation = chatService.getConversationById(conversationId);
            if (!conversation.getParticipantIds().contains(userId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("User is not a participant in this conversation")
                        .build()
                );
            }
            
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("File is empty")
                        .build()
                );
            }
            
            // Check file size (max 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("File size must be less than 10MB")
                        .build()
                );
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            String uniqueFilename = userId + "_" + System.currentTimeMillis() + fileExtension;
            
            // Save file (in a real implementation, you'd save to cloud storage)
            // For now, we'll just return a placeholder URL
            String fileUrl = "/uploads/chat/" + uniqueFilename;
            
            Map<String, String> responseData = new HashMap<>();
            responseData.put("fileUrl", fileUrl);
            responseData.put("fileName", originalFilename);
            
            ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                    .success(true)
                    .message("File uploaded successfully")
                    .data(responseData)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.<Map<String, String>>builder()
                    .success(false)
                    .message("Failed to upload file")
                    .build()
            );
        }
    }
}
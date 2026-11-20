package org.adnan.travner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.*;
import org.adnan.travner.dto.chat.ChatAttachmentDTO;
import org.adnan.travner.security.PrincipalIdentity;
import org.adnan.travner.service.ChatAttachmentService;
import org.adnan.travner.service.ChatService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat", description = "Chat management APIs")
public class ChatController {

    private final ChatService chatService;
    private final ChatAttachmentService chatAttachmentService;
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
        
        String userId = PrincipalIdentity.requireUserId(authentication);
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
        
        String currentUserId = PrincipalIdentity.requireUserId(authentication);
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
        
        String userId = PrincipalIdentity.requireUserId(authentication);

        // Participant check is enforced in the service for REST and STOMP paths.
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ChatMessageDTO> messages = chatService.getConversationMessages(conversationId, userId, pageable);
        
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
        
        String senderId = PrincipalIdentity.requireUserId(authentication);
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
        
        String userId = PrincipalIdentity.requireUserId(authentication);
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
        if (PrincipalIdentity.findUserId(principal).isEmpty()) {
            log.warn("Ignoring WebSocket message without an authenticated principal");
            return;
        }
        String senderId = PrincipalIdentity.requireUserId(principal);
        log.debug("Received WebSocket message from user: {} to conversation: {}", senderId, request.getConversationId());

        try {
            ChatMessageDTO message = chatService.sendMessage(request, senderId);

            // Broadcast to conversation participants
            messagingTemplate.convertAndSend("/topic/conversation/" + request.getConversationId(), message);

            // Personal queues are addressed by the principal NAME (display
            // username), which is what Spring's user-destination resolver uses.
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/messages", message);

        } catch (Exception e) {
            log.error("Error handling WebSocket message: {}", e.getMessage(), e);
        }
    }

    /**
     * WebSocket endpoint for typing indicators
     */
    @MessageMapping("/chat.typing")
    public void handleTypingIndicator(@Payload TypingIndicatorDTO typingIndicator, Principal principal) {
        if (PrincipalIdentity.findUserId(principal).isEmpty()) {
            log.warn("Ignoring typing indicator without an authenticated principal");
            return;
        }
        String senderId = PrincipalIdentity.requireUserId(principal);
        log.debug("Received typing indicator from user: {} in conversation: {}", senderId, typingIndicator.getConversationId());
        chatService.assertParticipant(typingIndicator.getConversationId(), senderId);

        // Identity uses the stable ID; the display name stays human-readable.
        typingIndicator.setUserId(senderId);
        typingIndicator.setUsername(principal.getName());
        
        // Broadcast typing indicator to conversation participants
        messagingTemplate.convertAndSend("/topic/conversation/" + typingIndicator.getConversationId() + "/typing", typingIndicator);
    }

    /**
     * Upload a validated file into a conversation. The returned attachment ID
     * is referenced by the subsequent FILE/IMAGE message.
     */
    @PostMapping("/upload")
    @Operation(summary = "Upload file for chat", description = "Store a validated file for sharing in a chat conversation")
    public ResponseEntity<ApiResponse<ChatAttachmentDTO>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("conversationId") String conversationId,
            Authentication authentication) {
        String userId = PrincipalIdentity.requireUserId(authentication);
        ChatAttachmentDTO attachment = chatAttachmentService.upload(file, conversationId, userId);
        return ResponseEntity.ok(ApiResponse.<ChatAttachmentDTO>builder()
                .success(true)
                .message("File uploaded successfully")
                .data(attachment)
                .build());
    }

    /** Authorized download: membership is enforced by the service. */
    @GetMapping("/attachments/{attachmentId}/download")
    @Operation(summary = "Download a chat attachment")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable String attachmentId,
            Authentication authentication) {
        String userId = PrincipalIdentity.requireUserId(authentication);
        ChatAttachmentService.Download download = chatAttachmentService.download(attachmentId, userId);
        String safeName = download.filename()
                .replace("\"", "").replace("\r", "").replace("\n", "");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeName + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(download.resource());
    }

    @GetMapping("/attachments/{attachmentId}")
    @Operation(summary = "Get chat attachment metadata")
    public ResponseEntity<ApiResponse<ChatAttachmentDTO>> getAttachment(
            @PathVariable String attachmentId,
            Authentication authentication) {
        String userId = PrincipalIdentity.requireUserId(authentication);
        return ResponseEntity.ok(ApiResponse.<ChatAttachmentDTO>builder()
                .success(true)
                .data(chatAttachmentService.getAttachment(attachmentId, userId))
                .build());
    }

    /** Only the uploader can remove an attachment; GridFS data is removed with it. */
    @DeleteMapping("/attachments/{attachmentId}")
    @Operation(summary = "Delete a chat attachment")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
            @PathVariable String attachmentId,
            Authentication authentication) {
        String userId = PrincipalIdentity.requireUserId(authentication);
        chatAttachmentService.delete(attachmentId, userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Attachment deleted")
                .build());
    }
}
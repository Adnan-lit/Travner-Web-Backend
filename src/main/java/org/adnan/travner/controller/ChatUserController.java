package org.adnan.travner.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.adnan.travner.dto.ApiResponse;
import org.adnan.travner.dto.UserSummaryDTO;
import org.adnan.travner.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * REST Controller for chat-related user operations
 */
@RestController
@RequestMapping("/api/chat/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat Users", description = "Chat-related user operations")
public class ChatUserController {

    private final UserService userService;

    /**
     * Get recent chat users (users you've chatted with recently)
     */
    @GetMapping("/recent")
    @Operation(summary = "Get recent chat users", description = "Get users you've chatted with recently")
    public ResponseEntity<ApiResponse<List<UserSummaryDTO>>> getRecentChatUsers(Authentication authentication) {
        log.debug("Getting recent chat users for user: {}", authentication.getName());

        // TODO: Implement chat history logic
        List<UserSummaryDTO> recentUsers = List.of(); // Placeholder

        ApiResponse<List<UserSummaryDTO>> response = ApiResponse.<List<UserSummaryDTO>>builder()
                .success(true)
                .message("Recent chat users retrieved successfully")
                .data(recentUsers)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get online users
     */
    @GetMapping("/online")
    @Operation(summary = "Get online users", description = "Get list of currently online users")
    public ResponseEntity<ApiResponse<List<UserSummaryDTO>>> getOnlineUsers(Authentication authentication) {
        log.debug("Getting online users for user: {}", authentication.getName());

        // TODO: Implement online users logic
        List<UserSummaryDTO> onlineUsers = List.of(); // Placeholder

        ApiResponse<List<UserSummaryDTO>> response = ApiResponse.<List<UserSummaryDTO>>builder()
                .success(true)
                .message("Online users retrieved successfully")
                .data(onlineUsers)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get suggested users for chat
     */
    @GetMapping("/suggested")
    @Operation(summary = "Get suggested users", description = "Get suggested users to start a chat with")
    public ResponseEntity<ApiResponse<List<UserSummaryDTO>>> getSuggestedUsers(Authentication authentication) {
        log.debug("Getting suggested users for user: {}", authentication.getName());

        // TODO: Implement suggested users logic
        List<UserSummaryDTO> suggestedUsers = List.of(); // Placeholder

        ApiResponse<List<UserSummaryDTO>> response = ApiResponse.<List<UserSummaryDTO>>builder()
                .success(true)
                .message("Suggested users retrieved successfully")
                .data(suggestedUsers)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get user's chat history summary
     */
    @GetMapping("/{userId}/chat-summary")
    @Operation(summary = "Get chat summary", description = "Get chat history summary with a specific user")
    public ResponseEntity<ApiResponse<ChatSummaryResponse>> getChatSummary(
            @PathVariable String userId,
            Authentication authentication) {
        log.debug("Getting chat summary between user {} and {}", authentication.getName(), userId);

        // TODO: Implement chat summary logic
        ChatSummaryResponse summary = new ChatSummaryResponse(); // Placeholder

        ApiResponse<ChatSummaryResponse> response = ApiResponse.<ChatSummaryResponse>builder()
                .success(true)
                .message("Chat summary retrieved successfully")
                .data(summary)
                .build();

        return ResponseEntity.ok(response);
    }

    // DTOs
    public static class ChatSummaryResponse {
        private String userId;
        private String username;
        private String firstName;
        private String lastName;
        private String profileImageUrl;
        private boolean isOnline;
        private long messageCount;
        private String lastMessage;
        private String lastMessageTime;
        private boolean hasUnreadMessages;
        private int unreadCount;

        // Constructors
        public ChatSummaryResponse() {}

        public ChatSummaryResponse(String userId, String username, String firstName, String lastName,
                                 String profileImageUrl, boolean isOnline, long messageCount,
                                 String lastMessage, String lastMessageTime, boolean hasUnreadMessages,
                                 int unreadCount) {
            this.userId = userId;
            this.username = username;
            this.firstName = firstName;
            this.lastName = lastName;
            this.profileImageUrl = profileImageUrl;
            this.isOnline = isOnline;
            this.messageCount = messageCount;
            this.lastMessage = lastMessage;
            this.lastMessageTime = lastMessageTime;
            this.hasUnreadMessages = hasUnreadMessages;
            this.unreadCount = unreadCount;
        }

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getProfileImageUrl() { return profileImageUrl; }
        public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

        public boolean isOnline() { return isOnline; }
        public void setOnline(boolean online) { isOnline = online; }

        public long getMessageCount() { return messageCount; }
        public void setMessageCount(long messageCount) { this.messageCount = messageCount; }

        public String getLastMessage() { return lastMessage; }
        public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

        public String getLastMessageTime() { return lastMessageTime; }
        public void setLastMessageTime(String lastMessageTime) { this.lastMessageTime = lastMessageTime; }

        public boolean isHasUnreadMessages() { return hasUnreadMessages; }
        public void setHasUnreadMessages(boolean hasUnreadMessages) { this.hasUnreadMessages = hasUnreadMessages; }

        public int getUnreadCount() { return unreadCount; }
        public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
    }
}

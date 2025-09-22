package org.adnan.travner.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.adnan.travner.domain.conversation.Conversation;
import org.adnan.travner.domain.conversation.ConversationMembership;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for conversation information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationResponse {

    private String id;
    private Conversation.ConversationType type;
    private String title;
    private List<ConversationMemberResponse> members;
    private String ownerId;
    private List<String> adminIds;
    private Instant createdAt;
    private Instant lastMessageAt;
    private boolean isArchived;
    private long unreadCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConversationMemberResponse {
        private String userId;
        private String userName;
        private String firstName;
        private String lastName;
        private ConversationMembership.MemberRole role;
        private Instant lastReadAt;
        private boolean muted;
        private Instant joinedAt;
    }
}
package org.adnan.travner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.adnan.travner.dto.chat.ChatParticipantDTO;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversationDTO {
    @Id
    private String id;
    /** Stable IDs, retained for compatibility and identity checks. */
    private List<String> participantIds;
    /** Resolved, display-safe participant summaries. */
    private List<ChatParticipantDTO> participants;
    private String title;
    private String type; // DIRECT, GROUP
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** Unread counts keyed by participant ID. */
    private Map<String, Integer> unreadCounts;
    /** Unread count for the requesting user; derived from unreadCounts. */
    private int unreadCount;
    private boolean isActive;
}

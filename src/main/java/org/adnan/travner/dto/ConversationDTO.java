package org.adnan.travner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO for conversations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationDTO {
    private String id;
    private String type;
    private String title;
    private List<String> memberIds;
    private String lastMessage;
    private Instant lastMessageAt;
    private int unreadCount;
    private Instant createdAt;
}






